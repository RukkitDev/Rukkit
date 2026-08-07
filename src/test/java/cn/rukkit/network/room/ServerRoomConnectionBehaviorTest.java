/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.room;

import cn.rukkit.Rukkit;
import cn.rukkit.config.RoundConfig;
import cn.rukkit.config.RukkitConfig;
import cn.rukkit.game.NetworkPlayer;
import cn.rukkit.network.ConnectionHandler;
import cn.rukkit.network.NetworkRoom;
import cn.rukkit.network.RoomConnection;
import cn.rukkit.network.command.GameCommand;
import cn.rukkit.service.ThreadManager;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the migrated connection keeps the master connection behavior
 * while its packets have moved to the core packet type.
 */
class ServerRoomConnectionBehaviorTest {
    private final List<EmbeddedChannel> channels = new ArrayList<>();
    private final List<ConnectionHandler> handlers = new ArrayList<>();
    private Object previousConfig;
    private Object previousRound;
    private Object previousThreadManager;
    private ThreadManager testThreadManager;

    @BeforeEach
    void installTestConfiguration() throws ReflectiveOperationException {
        RukkitConfig config = new RukkitConfig();
        config.maxPlayer = 2;
        config.useCommandQuere = false;
        previousConfig = setStatic("config", config);
        previousRound = setStatic("round", new RoundConfig());
        testThreadManager = new ThreadManager(1);
        previousThreadManager = setStatic("threadManager", testThreadManager);
    }

    @AfterEach
    void restoreConfiguration() throws ReflectiveOperationException {
        for (ConnectionHandler handler : handlers) {
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
    void connectionActionsKeepLegacyPacketWireFormat() throws Exception {
        LegacyFixture legacy = new LegacyFixture();
        MigratedFixture migrated = new MigratedFixture();
        configurePlayers(legacy.connection.player, migrated.connection.player);

        legacy.connection.sendChat("hello");
        migrated.connection.sendChat("hello");
        assertEquivalent(legacy.channel, migrated.channel);

        legacy.connection.sendServerMessage("server message");
        migrated.connection.sendServerMessage("server message");
        assertEquivalent(legacy.channel, migrated.channel);

        legacy.connection.sendMessage("Alice", "team message", 1);
        migrated.connection.sendMessage("Alice", "team message", 1);
        assertEquivalent(legacy.channel, migrated.channel);

        legacy.connection.kick("bye");
        migrated.connection.kick("bye");
        assertEquivalent(legacy.channel, migrated.channel);

        legacy.connection.lastSyncTick = 42;
        migrated.connection.lastSyncTick = 42;
        legacy.connection.doChecksum();
        migrated.connection.doChecksum();
        assertEquivalent(legacy.channel, migrated.channel);

        GameCommand command = new GameCommand();
        command.arr = new byte[] {1, 3, 5, 7};
        legacy.connection.currectRoom.currentStep = 42;
        migrated.connection.currectRoom.currentStep = 42;
        legacy.connection.sendGameCommand(command);
        migrated.connection.sendGameCommand(command);
        assertEquivalent(legacy.channel, migrated.channel);
    }

    @Test
    void teamListKeepsLegacyPacketWireFormatInSimpleMode() throws Exception {
        LegacyFixture legacy = new LegacyFixture();
        MigratedFixture migrated = new MigratedFixture();
        configurePlayers(legacy.connection.player, migrated.connection.player);

        legacy.connection.updateTeamList(true);
        migrated.connection.updateTeamList(true);

        assertEquivalent(legacy.channel, migrated.channel);
    }

    @Test
    void pingTaskUsesLegacyHeartbeatShape() {
        LegacyFixture legacy = new LegacyFixture();
        MigratedFixture migrated = new MigratedFixture();

        legacy.connection.new PingTasker().run();
        migrated.connection.new PingTasker().run();

        cn.rukkit.network.packet.Packet legacyPacket = legacy.channel.readOutbound();
        cn.rukkit.network.core.packet.Packet migratedPacket = migrated.channel.readOutbound();
        assertNotNull(legacyPacket);
        assertNotNull(migratedPacket);
        assertEquals(legacyPacket.type, migratedPacket.type);
        assertEquals(9, legacyPacket.bytes.length);
        assertEquals(9, migratedPacket.bytes.length);
        assertTrue(legacy.connection.pingTime > 0);
        assertTrue(migrated.connection.pingTime > 0);
    }

    private void configurePlayers(NetworkPlayer legacyPlayer, NetworkPlayer migratedPlayer) {
        for (NetworkPlayer player : new NetworkPlayer[] {legacyPlayer, migratedPlayer}) {
            player.name = "Alice";
            player.credits = 1234;
            player.team = 0;
            player.ping = 37;
        }
    }

    private static void assertEquivalent(EmbeddedChannel legacyChannel,
                                          EmbeddedChannel migratedChannel) {
        cn.rukkit.network.packet.Packet legacyPacket = legacyChannel.readOutbound();
        cn.rukkit.network.core.packet.Packet migratedPacket = migratedChannel.readOutbound();
        assertNotNull(legacyPacket);
        assertNotNull(migratedPacket);
        assertEquals(legacyPacket.type, migratedPacket.type);
        assertArrayEquals(legacyPacket.bytes, migratedPacket.bytes);
    }

    private ConnectionHandler newHandler() {
        ConnectionHandler handler = new ConnectionHandler();
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channels.add(channel);
        handlers.add(handler);
        return handler;
    }

    private static Object setStatic(String name, Object value) throws ReflectiveOperationException {
        Field field = Rukkit.class.getDeclaredField(name);
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, value);
        return previous;
    }

    private final class LegacyFixture {
        private final EmbeddedChannel channel;
        private final RoomConnection connection;

        private LegacyFixture() {
            NetworkRoom room = new NetworkRoom(1);
            ConnectionHandler handler = newHandler();
            channel = channels.get(channels.size() - 1);
            connection = new RoomConnection(handler, room);
            connection.player = new NetworkPlayer(connection);
            room.connectionManager.add(connection);
        }
    }

    private final class MigratedFixture {
        private final EmbeddedChannel channel;
        private final ServerRoomConnection connection;

        private MigratedFixture() {
            ServerRoom room = new ServerRoom(1);
            ConnectionHandler handler = newHandler();
            channel = channels.get(channels.size() - 1);
            connection = new ServerRoomConnection(handler, room);
            connection.player = new NetworkPlayer(connection);
            room.connectionManager.add(connection);
        }
    }
}
