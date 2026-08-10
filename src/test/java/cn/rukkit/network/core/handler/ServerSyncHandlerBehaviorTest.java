/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 */

package cn.rukkit.network.core.handler;

import cn.rukkit.Rukkit;
import cn.rukkit.config.RoundConfig;
import cn.rukkit.config.RukkitConfig;
import cn.rukkit.game.SaveData;
import cn.rukkit.network.ConnectionState;
import cn.rukkit.network.core.packet.Packet;
import cn.rukkit.network.core.packet.PacketType;
import cn.rukkit.network.io.GameInputStream;
import cn.rukkit.network.io.GameOutputStream;
import cn.rukkit.network.room.ServerGlobalConnectionManager;
import cn.rukkit.network.room.ServerRoom;
import cn.rukkit.network.room.ServerRoomConnection;
import cn.rukkit.network.room.ServerRoomManager;
import cn.rukkit.service.ThreadManager;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerSyncHandlerBehaviorTest {
    private final List<EmbeddedChannel> channels = new ArrayList<>();
    private final List<ServerConnectionHandler> handlers = new ArrayList<>();
    private Object previousConfig;
    private Object previousRound;
    private Object previousThreadManager;
    private ThreadManager testThreadManager;
    private ServerRoomManager roomManager;
    private ServerGlobalConnectionManager globalConnectionManager;

    @BeforeEach
    void installTestRuntime() throws ReflectiveOperationException {
        RukkitConfig config = new RukkitConfig();
        config.maxRoom = 1;
        config.maxPlayer = 2;
        previousConfig = setStatic("config", config);
        previousRound = setStatic("round", new RoundConfig());
        testThreadManager = new ThreadManager(2);
        previousThreadManager = setStatic("threadManager", testThreadManager);

        roomManager = new ServerRoomManager(Rukkit.getRoundConfig(), 1);
        globalConnectionManager = new ServerGlobalConnectionManager(roomManager);
    }

    @AfterEach
    void restoreTestRuntime() throws ReflectiveOperationException {
        for (ServerConnectionHandler handler : handlers) {
            handler.stopTimeout();
        }
        for (EmbeddedChannel channel : channels) {
            channel.finishAndReleaseAll();
        }
        testThreadManager.shutdown();

        setStatic("config", previousConfig);
        setStatic("round", previousRound);
        setStatic("threadManager", previousThreadManager);
    }

    @Test
    void storesLargeSyncPayloadWithMasterTimeConversion() throws Exception {
        ConnectionFixture fixture = newConnection();
        byte[] save = new byte[21];
        for (int i = 0; i < save.length; i++) {
            save[i] = (byte) (i + 1);
        }

        fixture.channel.writeInbound(syncPacket(300, 150, save));

        SaveData actual = fixture.connection.save;
        assertNotNull(actual);
        assertEquals(10, actual.time);
        assertArrayEquals(save, actual.arr);
    }

    @Test
    void ignoresSyncPayloadsAtOrBelowMasterMinimumSize() throws Exception {
        ConnectionFixture fixture = newConnection();
        SaveData previous = new SaveData();
        previous.arr = new byte[]{9, 8, 7};
        previous.time = 4;
        fixture.connection.save = previous;

        fixture.channel.writeInbound(syncPacket(300, 150, new byte[20]));

        assertEquals(previous, fixture.connection.save);
    }

    @Test
    void recordsChecksumValuesAndNotifiesRoom() throws Exception {
        ConnectionFixture fixture = newConnection();

        fixture.channel.writeInbound(checksumResponsePacket(
                300, 456, new long[]{111L, 222L, 333L}));

        assertEquals(456, fixture.connection.lastSyncTick);
        assertTrue(fixture.connection.checkSumSent);
        assertEquals(1, fixture.room.checkSumReceived.get());
        assertEquals(111L, fixture.connection.player.checkList.get(0).getCheckData());
        assertEquals(222L, fixture.connection.player.checkList.get(1).getCheckData());
        assertEquals(333L, fixture.connection.player.checkList.get(2).getCheckData());
    }

    @Test
    void requestsChecksumAgainWhenClientDoesNotSendValues() throws Exception {
        ConnectionFixture fixture = newConnection();

        fixture.channel.writeInbound(checksumResponseWithoutValues(300, 789));

        Packet retry = fixture.channel.readOutbound();
        assertNotNull(retry);
        assertEquals(PacketType.SYNC_CHECKSUM, retry.type);
        assertEquals(789, new GameInputStream(retry).readInt());
        assertEquals(789, fixture.connection.lastSyncTick);
    }

    @Test
    void syncHandlersOnlyAcceptInGameConnections() {
        assertEquals(List.of(ConnectionState.IN_GAME),
                new ServerSyncHandler().getAllowedStates());
        assertEquals(List.of(ConnectionState.IN_GAME),
                new ServerSyncChecksumResponseHandler().getAllowedStates());
    }

    private ConnectionFixture newConnection() {
        ServerPacketHandlerManager handlerManager = new ServerPacketHandlerManager();
        handlerManager.registerInternalHandler(roomManager, globalConnectionManager);
        ServerConnectionHandler handler = new ServerConnectionHandler(
                handlerManager, globalConnectionManager::discard);
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        handlers.add(handler);
        channels.add(channel);

        ServerRoom room = roomManager.getRoom(0);
        ServerRoomConnection connection = new ServerRoomConnection(handler, room);
        connection.player = new cn.rukkit.game.NetworkPlayer(connection);
        connection.player.name = "Alice";
        connection.player.uuid = "sync-handler-test";
        room.connectionManager.add(connection);
        globalConnectionManager.add(connection);
        handler.setConn(connection);
        handler.setState(ConnectionState.IN_GAME);
        return new ConnectionFixture(handler, channel, room, connection);
    }

    private static Packet syncPacket(int frame, int time, byte[] save) throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeByte(0);
        output.writeInt(frame);
        output.writeInt(time);
        output.writeFloat(1.0f);
        output.writeFloat(1.0f);
        output.writeBoolean(false);
        output.writeBoolean(false);
        output.startBlock("gameSave", false);
        output.write(save);
        output.endBlock();
        return output.createPacket(PacketType.SYNC);
    }

    private static Packet checksumResponsePacket(int serverTick, int clientTick,
                                                 long[] values) throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeByte(0);
        output.writeInt(serverTick);
        output.writeInt(clientTick);
        output.writeBoolean(true);
        output.writeLong(0L);
        output.writeLong(0L);
        output.startBlock("checksum", false);
        output.writeInt(0);
        output.writeInt(values.length);
        for (long value : values) {
            output.writeLong(0L);
            output.writeLong(value);
        }
        output.endBlock();
        return output.createPacket(PacketType.SYNC_CHECKSUM_RESPONSE);
    }

    private static Packet checksumResponseWithoutValues(int serverTick, int clientTick)
            throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeByte(0);
        output.writeInt(serverTick);
        output.writeInt(clientTick);
        output.writeBoolean(false);
        return output.createPacket(PacketType.SYNC_CHECKSUM_RESPONSE);
    }

    private static Object setStatic(String name, Object value)
            throws ReflectiveOperationException {
        Field field = Rukkit.class.getDeclaredField(name);
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, value);
        return previous;
    }

    private record ConnectionFixture(ServerConnectionHandler handler,
                                     EmbeddedChannel channel,
                                     ServerRoom room,
                                     ServerRoomConnection connection) {
    }
}
