/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 */

package cn.rukkit.network.core.handler;

import cn.rukkit.Rukkit;
import cn.rukkit.config.RoundConfig;
import cn.rukkit.config.RukkitConfig;
import cn.rukkit.event.EventHandler;
import cn.rukkit.event.EventListener;
import cn.rukkit.event.EventListenerContainer;
import cn.rukkit.event.action.BuildEvent;
import cn.rukkit.game.GameActions;
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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerAddGameCommandHandlerBehaviorTest {
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
        config.useCommandQuere = false;
        previousConfig = setStatic("config", config);
        previousRound = setStatic("round", new RoundConfig());
        testThreadManager = new ThreadManager(2);
        previousThreadManager = setStatic("threadManager", testThreadManager);

        roomManager = new ServerRoomManager(Rukkit.getRoundConfig(), 1);
        globalConnectionManager = new ServerGlobalConnectionManager(roomManager);
        BuildEvent.getListenerList().clear();
    }

    @AfterEach
    void restoreTestRuntime() throws ReflectiveOperationException {
        BuildEvent.getListenerList().clear();
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
    void rewritesMinimalCommandAndBroadcastsItAsTick() throws Exception {
        ConnectionFixture fixture = newConnection();

        fixture.channel.writeInbound(addGameCommandPacket(minimalCommand()));

        Packet tick = fixture.channel.readOutbound();
        assertNotNull(tick);
        assertEquals(PacketType.TICK, tick.type);

        GameInputStream output = new GameInputStream(tick);
        assertEquals(0, output.readInt());
        assertEquals(1, output.readInt());
        assertEquals("c", output.readString());
        byte[] rewritten = output.readStreamBytes();
        assertTrue(rewritten.length > 0);

        GameInputStream command = new GameInputStream(rewritten);
        assertEquals(0, command.readByte());
        assertFalse(command.readBoolean());
        assertFalse(command.readBoolean());
        assertFalse(command.readBoolean());
        assertEquals(0, command.readInt());
        assertEquals(0, command.readInt());
        assertFalse(command.readBoolean());
        assertFalse(command.readBoolean());
        assertEquals(0, command.readInt());
        assertTrue(command.readBoolean());
        assertEquals(fixture.connection.player.playerIndex, command.readByte());
        assertFalse(command.readBoolean());
        assertEquals(-1L, command.readLong());
        assertEquals("-1", command.readString());
        assertFalse(command.readBoolean());
        assertEquals(0, command.readShort());
        assertFalse(command.readBoolean());
        assertEquals(0, command.readInt());
        assertFalse(command.readBoolean());
    }

    @Test
    void rebuildsSharedControlMaskFromCurrentRoomState() throws Exception {
        ConnectionFixture fixture = newConnection();
        fixture.connection.player.isSharingControl = true;

        fixture.channel.writeInbound(addGameCommandPacket(minimalCommand()));

        Packet tickPacket = fixture.channel.readOutbound();
        GameInputStream tick = new GameInputStream(tickPacket);
        tick.readInt();
        tick.readInt();
        tick.readString();
        GameInputStream command = new GameInputStream(tick.readStreamBytes());
        command.readByte();
        command.readBoolean();
        command.readBoolean();
        command.readBoolean();
        command.readInt();
        command.readInt();
        command.readBoolean();
        command.readBoolean();
        command.readInt();
        command.readBoolean();
        command.readByte();
        command.readBoolean();
        command.readLong();
        command.readString();
        command.readBoolean();
        assertEquals(1, command.readShort());
    }

    @Test
    void stripsSystemActionSubmittedByClient() throws Exception {
        ConnectionFixture fixture = newConnection();

        fixture.channel.writeInbound(addGameCommandPacket(systemActionCommand()));

        Packet tickPacket = fixture.channel.readOutbound();
        GameInputStream tick = new GameInputStream(tickPacket);
        tick.readInt();
        tick.readInt();
        tick.readString();
        GameInputStream command = new GameInputStream(tick.readStreamBytes());
        command.readByte();
        command.readBoolean();
        command.readBoolean();
        command.readBoolean();
        command.readInt();
        command.readInt();
        command.readBoolean();
        command.readBoolean();
        command.readInt();
        command.readBoolean();
        command.readBoolean();
        command.readByte();
        command.readLong();
        command.readString();
        command.readBoolean();
        command.readShort();
        assertFalse(command.readBoolean());
        assertEquals(0, command.readInt());
        assertFalse(command.readBoolean());
    }

    @Test
    void publishesBuildEventBeforeBroadcastingCommand() throws Exception {
        BuildProbe probe = new BuildProbe();
        Method method = BuildProbe.class.getDeclaredMethod("onBuild", BuildEvent.class);
        BuildEvent.getListenerList().registerListener(
                new EventListenerContainer(null, method, probe));
        ConnectionFixture fixture = newConnection();

        fixture.channel.writeInbound(addGameCommandPacket(buildCommand()));

        assertTrue(probe.called);
        assertEquals(fixture.connection.player, probe.player);
        assertEquals(1.5f, probe.x);
        assertEquals(2.5f, probe.y);
        assertEquals(99L, probe.unitId);
        assertEquals("custom-unit", probe.unitName);
        assertNotNull(fixture.channel.readOutbound());
    }

    @Test
    void addGameCommandOnlyAcceptsInGameConnections() {
        assertEquals(List.of(ConnectionState.IN_GAME),
                new ServerAddGameCommandHandler().getAllowedStates());
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
        connection.player.uuid = "add-command-handler-test";
        room.connectionManager.add(connection);
        globalConnectionManager.add(connection);
        handler.setConn(connection);
        handler.setState(ConnectionState.IN_GAME);
        return new ConnectionFixture(handler, channel, room, connection);
    }

    private static Packet addGameCommandPacket(byte[] command) throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.startBlock("c", false);
        output.write(command);
        output.endBlock();
        return output.createPacket(PacketType.ADD_GAMECOMMAND);
    }

    private static byte[] minimalCommand() throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeByte(0);
        output.writeBoolean(false);
        writeCommandTail(output);
        return output.createPacket(PacketType.TICK).bytes;
    }

    private static byte[] systemActionCommand() throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeByte(0);
        output.writeBoolean(false);
        writeCommandTail(output, true);
        return output.createPacket(PacketType.TICK).bytes;
    }

    private static byte[] buildCommand() throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeByte(0);
        output.writeBoolean(true);
        output.writeEnum(GameActions.BUILD);
        output.writeInt(-2);
        output.writeString("custom-unit");
        output.writeFloat(1.5f);
        output.writeFloat(2.5f);
        output.writeLong(99L);
        output.writeByte(0);
        output.writeFloat(0.0f);
        output.writeFloat(0.0f);
        output.writeBoolean(false);
        output.writeBoolean(false);
        output.writeBoolean(false);
        output.writeBoolean(false);
        writeCommandTail(output);
        return output.createPacket(PacketType.TICK).bytes;
    }

    private static void writeCommandTail(GameOutputStream output) throws IOException {
        writeCommandTail(output, false);
    }

    private static void writeCommandTail(GameOutputStream output, boolean systemAction)
            throws IOException {
        output.writeBoolean(false);
        output.writeBoolean(false);
        output.writeInt(0);
        output.writeInt(0);
        output.writeBoolean(false);
        output.writeBoolean(false);
        output.writeInt(0);
        output.writeBoolean(false);
        output.writeBoolean(false);
        output.writeLong(-1L);
        output.writeString("-1");
        output.writeBoolean(false);
        output.writeShort((short) 0);
        output.writeBoolean(systemAction);
        if (systemAction) {
            output.writeByte(7);
            output.writeFloat(1.0f);
            output.writeFloat(2.0f);
            output.writeInt(3);
        }
        output.writeInt(0);
        output.writeBoolean(false);
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

    private static final class BuildProbe implements EventListener {
        private boolean called;
        private cn.rukkit.game.NetworkPlayer player;
        private float x;
        private float y;
        private long unitId;
        private String unitName;

        @EventHandler
        public void onBuild(BuildEvent event) {
            called = true;
            player = event.getPlayer();
            x = event.getTargetX();
            y = event.getTargetY();
            unitId = event.getFromUnitId();
            unitName = event.getTargetUnitName();
        }
    }
}
