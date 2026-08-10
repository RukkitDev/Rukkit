/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.core.handler;

import cn.rukkit.Rukkit;
import cn.rukkit.config.RoundConfig;
import cn.rukkit.config.RukkitConfig;
import cn.rukkit.game.NetworkPlayer;
import cn.rukkit.game.mod.ModManager;
import cn.rukkit.network.ConnectionState;
import cn.rukkit.network.core.packet.Packet;
import cn.rukkit.network.core.packet.PacketType;
import cn.rukkit.network.core.packet.UniversalPacket;
import cn.rukkit.network.io.GameOutputStream;
import cn.rukkit.network.room.ServerGlobalConnectionManager;
import cn.rukkit.network.room.ServerRoom;
import cn.rukkit.network.room.ServerRoomConnection;
import cn.rukkit.network.room.ServerRoomManager;
import cn.rukkit.service.ThreadManager;
import io.netty.channel.embedded.EmbeddedChannel;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerPlayerInfoHandlerBehaviorTest {
    private final List<EmbeddedChannel> channels = new ArrayList<>();
    private final List<ServerConnectionHandler> handlers = new ArrayList<>();
    private Object previousConfig;
    private Object previousRound;
    private Object previousThreadManager;
    private Object previousModManager;
    private ThreadManager testThreadManager;
    private ServerRoomManager roomManager;
    private ServerGlobalConnectionManager globalConnectionManager;
    private boolean modsDirectoryExisted;

    @BeforeEach
    void installTestConfiguration() throws ReflectiveOperationException {
        RukkitConfig config = new RukkitConfig();
        config.maxRoom = 1;
        config.maxPlayer = 2;
        config.syncEnabled = true;
        config.isDebug = false;
        previousConfig = setStatic("config", config);
        previousRound = setStatic("round", new RoundConfig());
        testThreadManager = new ThreadManager(4);
        previousThreadManager = setStatic("threadManager", testThreadManager);

        File modsDirectory = new File(Rukkit.getEnvPath(), "mods");
        modsDirectoryExisted = modsDirectory.isDirectory();
        previousModManager = setStatic("modManager", new ModManager());

        roomManager = new ServerRoomManager(Rukkit.getRoundConfig(), 1);
        globalConnectionManager = new ServerGlobalConnectionManager(roomManager);
    }

    @AfterEach
    void restoreConfiguration() throws ReflectiveOperationException {
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
        setStatic("modManager", previousModManager);

        File modsDirectory = new File(Rukkit.getEnvPath(), "mods");
        if (!modsDirectoryExisted && modsDirectory.isDirectory()) {
            modsDirectory.delete();
        }
    }

    @Test
    void registersFirstPlayerIntoNewRoomAndGlobalRegistry() throws Exception {
        ConnectionFixture fixture = registerPlayer("Alice", "uuid-first");

        ServerRoomConnection connection = fixture.handler.getConn();
        assertNotNull(connection);
        assertEquals(1, fixture.room.playerManager.getPlayerCount());
        assertEquals(1, globalConnectionManager.size());
        assertEquals("Alice", connection.player.name);
        assertEquals("uuid-first", connection.player.uuid);
        assertTrue(connection.player.isAdmin);
        assertEquals(ConnectionState.IN_ROOM, fixture.handler.getState());

        List<Integer> packetTypes = drainPacketTypes(fixture.channel);
        assertTrue(packetTypes.contains(PacketType.SERVER_INFO));
        assertTrue(packetTypes.contains(PacketType.TEAM_LIST));
        assertTrue(packetTypes.contains(PacketType.SEND_CHAT));
    }

    @Test
    void completesPreRegistrationBeforePlayerInfo() throws Exception {
        ConnectionFixture fixture = newConnection();

        fixture.channel.writeInbound(new Packet(PacketType.PREREGISTER_CONNECTION, new byte[0]));

        assertEquals(ConnectionState.PRE_REGISTERED, fixture.handler.getState());
        Packet registrationPacket = fixture.channel.readOutbound();
        Packet promptPacket = fixture.channel.readOutbound();
        assertEquals(PacketType.REGISTER_CONNECTION, registrationPacket.type);
        assertEquals(PacketType.SEND_CHAT, promptPacket.type);

        fixture.channel.writeInbound(playerInfoPacket("Alice", "uuid-handshake"));

        assertEquals(ConnectionState.IN_ROOM, fixture.handler.getState());
        assertNotNull(fixture.handler.getConn());
    }

    @Test
    void updatesPlayerPingInRoomAndGameStates() throws Exception {
        ConnectionFixture fixture = registerPlayer("Alice", "uuid-heartbeat");
        ServerRoomConnection connection = fixture.handler.getConn();

        for (ConnectionState state : List.of(ConnectionState.IN_ROOM, ConnectionState.IN_GAME)) {
            fixture.handler.setState(state);
            connection.pingTime = System.currentTimeMillis() - 25;

            fixture.channel.writeInbound(new Packet(PacketType.HEART_BEAT_RESPONSE, new byte[0]));

            assertTrue(connection.player.ping >= 0);
            assertTrue(connection.player.ping < 1000);
            assertEquals(state, fixture.handler.getState());
        }
    }

    @Test
    void broadcastsOrdinaryChatThroughMigratedRoom() throws Exception {
        ConnectionFixture fixture = registerPlayer("Alice", "uuid-chat");
        drainPacketTypes(fixture.channel);

        fixture.channel.writeInbound(chatPacket("hello"));

        Packet actual = fixture.channel.readOutbound();
        Packet expected = UniversalPacket.chat("Alice", "hello", fixture.handler.getConn().player.playerIndex);
        assertNotNull(actual);
        assertEquals(PacketType.SEND_CHAT, actual.type);
        assertArrayEquals(expected.bytes, actual.bytes);
    }

    @Test
    void forwardsChatCommandToInjectedCoreDispatcher() throws Exception {
        AtomicReference<ServerRoomConnection> receivedConnection = new AtomicReference<>();
        AtomicReference<String> receivedCommand = new AtomicReference<>();
        ConnectionFixture fixture = newConnection((connection, command) -> {
            receivedConnection.set(connection);
            receivedCommand.set(command);
        });
        fixture.handler.setState(ConnectionState.PRE_REGISTERED);
        fixture.channel.writeInbound(playerInfoPacket("Alice", "uuid-command"));
        drainPacketTypes(fixture.channel);

        fixture.channel.writeInbound(chatPacket(".version now"));

        assertSame(fixture.handler.getConn(), receivedConnection.get());
        assertEquals("version now", receivedCommand.get());
    }

    @Test
    void ignoresHeartbeatWithoutARegisteredConnection() {
        ConnectionFixture fixture = newConnection();
        fixture.handler.setState(ConnectionState.IN_ROOM);

        fixture.channel.writeInbound(new Packet(PacketType.HEART_BEAT_RESPONSE, new byte[0]));

        assertTrue(fixture.channel.isOpen());
        assertEquals(ConnectionState.IN_ROOM, fixture.handler.getState());
    }

    @Test
    void rejectsDuplicatePlayerWhileRoomIsNotGaming() throws Exception {
        ConnectionFixture first = registerPlayer("Alice", "uuid-duplicate");
        drainPacketTypes(first.channel);

        ConnectionFixture duplicate = newConnection();
        duplicate.handler.setState(ConnectionState.PRE_REGISTERED);
        duplicate.channel.writeInbound(playerInfoPacket("Alice-2", "uuid-duplicate"));

        assertNull(duplicate.handler.getConn());
        assertEquals(1, globalConnectionManager.size());
        assertEquals(1, duplicate.room.playerManager.getPlayerCount());
        assertTrue(drainPacketTypes(duplicate.channel).contains(PacketType.KICK));
    }

    @Test
    void rejectsNewPlayerWhenSelectedRoomIsAlreadyGaming() throws Exception {
        TestServerRoom room = new TestServerRoom(0);
        room.currentStep = 10;
        roomManager = new FixedRoomManager(room);
        globalConnectionManager = new ServerGlobalConnectionManager(roomManager);

        ConnectionFixture fixture = newConnection();
        fixture.handler.setState(ConnectionState.PRE_REGISTERED);
        fixture.channel.writeInbound(playerInfoPacket("Alice", "uuid-game-started"));

        assertNull(fixture.handler.getConn());
        assertEquals(0, room.playerManager.getPlayerCount());
        assertEquals(0, room.connectionManager.size());
        assertEquals(0, globalConnectionManager.size());
        assertTrue(drainPacketTypes(fixture.channel).contains(PacketType.KICK));
    }

    @Test
    void doesNotPublishConnectionWhenPlayerSlotsAreFull() throws Exception {
        Rukkit.getConfig().maxPlayer = 1;
        roomManager = new ServerRoomManager(Rukkit.getRoundConfig(), 1);
        globalConnectionManager = new ServerGlobalConnectionManager(roomManager);

        ConnectionFixture first = registerPlayer("Alice", "uuid-capacity-first");
        ConnectionFixture spare = newConnection();
        ServerRoomConnection extra = new ServerRoomConnection(spare.handler, first.room);
        extra.player = new NetworkPlayer(extra);
        extra.player.name = "Bob";
        extra.player.uuid = "uuid-capacity-second";

        first.room.connectionManager.add(extra);

        assertEquals(1, first.room.playerManager.getPlayerCount());
        assertEquals(1, first.room.connectionManager.getConnections().size());
        assertEquals(1, first.room.connectionManager.size());
    }

    @Test
    void reconnectsDisconnectedPlayerToTheNewConnectionInGame() throws Exception {
        TestServerRoom room = new TestServerRoom(0);
        roomManager.roomList.set(0, room);
        ConnectionFixture first = registerPlayer("Alice", "uuid-reconnect");
        NetworkPlayer player = first.handler.getConn().player;

        room.currentStep = 10;
        first.channel.close();

        assertEquals(0, globalConnectionManager.size());
        assertEquals(0, room.connectionManager.size());
        assertTrue(player.isDisconnected);

        ConnectionFixture reconnect = newConnection();
        reconnect.handler.setState(ConnectionState.PRE_REGISTERED);
        reconnect.channel.writeInbound(playerInfoPacket("Alice-Reconnected", "uuid-reconnect"));

        assertSame(player, reconnect.handler.getConn().player);
        assertSame(reconnect.handler.getConn(), player.getServerConnection());
        assertEquals("Alice-Reconnected", player.name);
        assertFalse(player.isDisconnected);
        assertEquals(1, room.connectionManager.size());
        assertEquals(1, globalConnectionManager.size());
        assertEquals(ConnectionState.IN_GAME, reconnect.handler.getState());
    }

    @Test
    void oldConnectionClosingAfterReconnectDoesNotDisconnectReplacement() throws Exception {
        TestServerRoom room = new TestServerRoom(0);
        roomManager.roomList.set(0, room);
        ConnectionFixture first = registerPlayer("Alice", "uuid-overlapping-reconnect");
        NetworkPlayer player = first.handler.getConn().player;

        room.currentStep = 10;
        ConnectionFixture reconnect = newConnection();
        reconnect.handler.setState(ConnectionState.PRE_REGISTERED);
        reconnect.channel.writeInbound(
                playerInfoPacket("Alice-Reconnected", "uuid-overlapping-reconnect"));

        assertSame(player, reconnect.handler.getConn().player);
        assertEquals(1, room.connectionManager.getConnections().size());
        assertEquals(1, globalConnectionManager.getConnections().size());

        first.channel.close();

        assertSame(reconnect.handler.getConn(), player.getServerConnection());
        assertFalse(player.isDisconnected);
        assertEquals(1, room.connectionManager.getConnections().size(),
                room.connectionManager.getConnections().toString());
        assertEquals(1, globalConnectionManager.getConnections().size());
    }

    @Test
    void transfersAdminToLivePlayerWhenAdminLeavesDuringGame() throws Exception {
        ConnectionFixture admin = registerPlayer("Admin", "uuid-admin");
        ConnectionFixture survivor = registerPlayer("Survivor", "uuid-survivor");
        admin.handler.getConn().player.isAdmin = true;
        survivor.handler.getConn().player.isAdmin = false;
        admin.room.currentStep = 10;

        admin.channel.close();

        assertFalse(admin.handler.getConn().player.isAdmin);
        assertTrue(survivor.handler.getConn().player.isAdmin);
        assertEquals(1, admin.room.connectionManager.getConnections().size());
        assertEquals(1, globalConnectionManager.getConnections().size());
    }

    private ConnectionFixture registerPlayer(String name, String uuid) throws Exception {
        ConnectionFixture fixture = newConnection();
        fixture.handler.setState(ConnectionState.PRE_REGISTERED);
        fixture.channel.writeInbound(playerInfoPacket(name, uuid));
        return fixture;
    }

    private ConnectionFixture newConnection() {
        return newConnection(null);
    }

    private ConnectionFixture newConnection(ServerChatCommandDispatcher commandDispatcher) {
        ServerPacketHandlerManager handlerManager = new ServerPacketHandlerManager();
        handlerManager.registerInternalHandler(roomManager, globalConnectionManager, commandDispatcher);
        ServerConnectionHandler handler = new ServerConnectionHandler(
                handlerManager, globalConnectionManager::discard);
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        handlers.add(handler);
        channels.add(channel);
        return new ConnectionFixture(handler, channel, roomManager.getRoom(0));
    }

    private static Packet playerInfoPacket(String name, String uuid) throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeString("com.corrodinggames.rts");
        output.writeInt(1);
        output.writeInt(176);
        output.writeInt(176);
        output.writeString(name);
        output.writeByte(0);
        output.writeString("");
        output.writeString(uuid);
        output.writeInt(0);
        output.writeString("");
        return output.createPacket(PacketType.PLAYER_INFO);
    }

    private static Packet chatPacket(String message) throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeString(message);
        return output.createPacket(PacketType.ADD_CHAT);
    }

    private static List<Integer> drainPacketTypes(EmbeddedChannel channel) {
        List<Integer> types = new ArrayList<>();
        Packet packet;
        while ((packet = channel.readOutbound()) != null) {
            types.add(packet.type);
        }
        return types;
    }

    private static Object setStatic(String name, Object value) throws ReflectiveOperationException {
        Field field = Rukkit.class.getDeclaredField(name);
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, value);
        return previous;
    }

    private record ConnectionFixture(ServerConnectionHandler handler,
                                     EmbeddedChannel channel,
                                     ServerRoom room) {
    }

    private static final class FixedRoomManager extends ServerRoomManager {
        private final ServerRoom selectedRoom;

        private FixedRoomManager(ServerRoom selectedRoom) {
            super(Rukkit.getRoundConfig(), 1);
            this.selectedRoom = selectedRoom;
            roomList.set(0, selectedRoom);
        }

        @Override
        public ServerRoom getAvailableRoom() {
            return selectedRoom;
        }
    }

    private static final class TestServerRoom extends ServerRoom {
        private TestServerRoom(int id) {
            super(id);
        }

        @Override
        public void syncGame() {
        }
    }
}
