/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.plugin.internal;

import cn.rukkit.Rukkit;
import cn.rukkit.command.CommandManager;
import cn.rukkit.config.RoundConfig;
import cn.rukkit.config.RukkitConfig;
import cn.rukkit.game.NetworkPlayer;
import cn.rukkit.game.mod.ModManager;
import cn.rukkit.game.map.OfficialMap;
import cn.rukkit.network.ConnectionHandler;
import cn.rukkit.network.ConnectionState;
import cn.rukkit.network.NetworkRoom;
import cn.rukkit.network.RoomConnection;
import cn.rukkit.network.core.handler.ServerConnectionHandler;
import cn.rukkit.network.core.handler.ServerPacketHandlerManager;
import cn.rukkit.network.core.packet.Packet;
import cn.rukkit.network.core.packet.PacketType;
import cn.rukkit.network.io.GameInputStream;
import cn.rukkit.network.room.ServerRoom;
import cn.rukkit.network.room.ServerRoomConnection;
import cn.rukkit.plugin.PluginManager;
import cn.rukkit.plugin.RukkitPlugin;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandPluginSimpleCommandMigrationTest {
    private final List<EmbeddedChannel> channels = new ArrayList<>();
    private final List<ServerConnectionHandler> serverHandlers = new ArrayList<>();
    private final List<ConnectionHandler> legacyHandlers = new ArrayList<>();
    private Object previousConfig;
    private Object previousRound;
    private Object previousThreadManager;
    private Object previousModManager;
    private Object previousCommandManager;
    private Object previousPluginManager;
    private ThreadManager testThreadManager;
    private CoreCommandPlugin coreCommandPlugin;
    private CommandPlugin legacyCommandPlugin;

    @BeforeEach
    void installTestConfiguration() throws ReflectiveOperationException {
        RukkitConfig config = new RukkitConfig();
        config.maxRoom = 1;
        config.maxPlayer = 2;
        config.threadPoolCount = 3;
        previousConfig = setStatic("config", config);
        previousRound = setStatic("round", new RoundConfig());
        previousModManager = setStatic("modManager", new ModManager());
        testThreadManager = new ThreadManager(2);
        previousThreadManager = setStatic("threadManager", testThreadManager);
        previousCommandManager = setStatic("commandManager", new CommandManager());
        previousPluginManager = setStatic("pluginManager", new PluginManager());

        coreCommandPlugin = new CoreCommandPlugin();
        coreCommandPlugin.onLoad();
    }

    @AfterEach
    void restoreConfiguration() throws ReflectiveOperationException {
        unregisterPluginListeners();
        for (ServerConnectionHandler handler : serverHandlers) {
            handler.stopTimeout();
        }
        for (ConnectionHandler handler : legacyHandlers) {
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
        setStatic("commandManager", previousCommandManager);
        setStatic("pluginManager", previousPluginManager);
    }

    @Test
    void registersContextListenersForSimpleCommands() {
        CommandManager manager = Rukkit.getCommandManager();

        assertNotNull(manager.fetchCommand("version").getContextListener());
        assertNotNull(manager.fetchCommand("state").getContextListener());
        assertNotNull(manager.fetchCommand("help").getContextListener());
        assertNull(manager.fetchCommand("version").getListener());
        assertEquals(coreCommandPlugin, manager.fetchCommand("version").getContextListener());
    }

    @Test
    void registersContextListenersForLowRiskRoomCommands() {
        CommandManager manager = Rukkit.getCommandManager();

        assertNotNull(manager.fetchCommand("t").getContextListener());
        assertNotNull(manager.fetchCommand("self_team").getContextListener());
        assertNotNull(manager.fetchCommand("chksum").getContextListener());
        assertNotNull(manager.fetchCommand("maping").getContextListener());
        assertNotNull(manager.fetchCommand("list").getContextListener());
        assertNotNull(manager.fetchCommand("surrender").getContextListener());
    }

    @Test
    void everyCoreCommandRegisteredByCoreCommandPluginHasContextBehavior() {
        CommandManager manager = Rukkit.getCommandManager();

        assertTrue(manager.getLoadedCommand().values().stream()
                .allMatch(command -> command.getContextListener() != null));
    }

    @Test
    void executesSimpleCommandsThroughMigratedConnection() throws Exception {
        ServerConnectionFixture fixture = newServerConnection();
        CommandManager manager = Rukkit.getCommandManager();

        manager.executeChatCommand(fixture.connection, "version");
        Packet version = readServerMessage(fixture.channel);
        assertEquals("Rukkit Server v" + cn.rukkit.util.VersionUtil.getVersion()
                        + "\nRukkit Plugin API v" + Rukkit.PLUGIN_API_VERSION,
                readChatMessage(version));

        manager.executeChatCommand(fixture.connection, "help");
        String help = readChatMessage(readServerMessage(fixture.channel));
        assertTrue(help.startsWith("- Help -  Page 1"));
        assertTrue(help.contains(" : "));

        manager.executeChatCommand(fixture.connection, "state");
        String state = readChatMessage(readServerMessage(fixture.channel));
        assertTrue(state.contains("Connections: 1"));
        assertTrue(state.contains("ThreadManager Tasks: "));
        assertTrue(state.endsWith("/3"));
    }

    @Test
    void qcAcceptsOptionalPrefixThroughMigratedConnection() throws Exception {
        ServerConnectionFixture fixture = newServerConnection();
        CommandManager manager = Rukkit.getCommandManager();
        String expected = "Rukkit Server v" + cn.rukkit.util.VersionUtil.getVersion()
                + "\nRukkit Plugin API v" + Rukkit.PLUGIN_API_VERSION;

        for (String prefix : new String[]{"", ".", "-"}) {
            manager.executeChatCommand(fixture.connection, "qc " + prefix + "version");
            assertEquals(expected, readChatMessage(readServerMessage(fixture.channel)));
        }
    }

    @Test
    void listsOfficialMapsInNonOverlappingTenItemPagesThroughMigratedConnection()
            throws Exception {
        ServerConnectionFixture fixture = newServerConnection();
        CommandManager manager = Rukkit.getCommandManager();

        manager.executeChatCommand(fixture.connection, "maps 1");
        String firstPage = readChatMessage(readServerMessage(fixture.channel));
        assertTrue(firstPage.contains("[0] " + OfficialMap.maps[0]));
        assertTrue(firstPage.contains("[9] " + OfficialMap.maps[9]));
        assertFalse(firstPage.contains("[10] " + OfficialMap.maps[10]));

        manager.executeChatCommand(fixture.connection, "maps 2");
        String secondPage = readChatMessage(readServerMessage(fixture.channel));
        assertFalse(secondPage.contains("[9] " + OfficialMap.maps[9]));
        assertTrue(secondPage.contains("[10] " + OfficialMap.maps[10]));
        assertTrue(secondPage.contains("[19] " + OfficialMap.maps[19]));
        assertFalse(secondPage.contains("[20] " + OfficialMap.maps[20]));
    }

    @Test
    void selectsOfficialMapByTheDisplayedNameThroughMigratedConnection() throws Exception {
        ServerConnectionFixture fixture = newServerConnection();
        fixture.connection.player.isAdmin = true;

        Rukkit.getCommandManager().executeChatCommand(
                fixture.connection, "map '" + OfficialMap.maps[0] + "'");

        assertEquals(OfficialMap.maps[0], fixture.connection.currectRoom.config.mapName);
        assertServerInfoPair(fixture.channel);
    }

    @Test
    void listsEmptyCustomMapDirectoryWithoutFailingThroughMigratedConnection()
            throws Exception {
        ServerConnectionFixture fixture = newServerConnection();

        Rukkit.getCommandManager().executeChatCommand(fixture.connection, "cmaps");
        String message = readChatMessage(readServerMessage(fixture.channel));
        assertTrue(message.startsWith("- Help -  Page 1"));
    }

    @Test
    void keepsVersionAvailableThroughLegacyConnection() throws Exception {
        installLegacyCommands();
        LegacyConnectionFixture fixture = newLegacyConnection();

        Rukkit.getCommandManager().executeChatCommand(fixture.connection, "version");

        cn.rukkit.network.packet.Packet actual = fixture.channel.readOutbound();
        assertNotNull(actual);
        assertArrayEquals(
                cn.rukkit.network.packet.Packet.chat(
                        "SERVER",
                        "Rukkit Server v" + cn.rukkit.util.VersionUtil.getVersion()
                                + "\nRukkit Plugin API v" + Rukkit.PLUGIN_API_VERSION,
                        -1).bytes,
                actual.bytes);
    }

    @Test
    void keepsMapCommandBehaviorThroughLegacyConnection() throws Exception {
        installLegacyCommands();
        LegacyConnectionFixture fixture = newLegacyConnection();
        CommandManager manager = Rukkit.getCommandManager();

        manager.executeChatCommand(fixture.connection, "maps 1");
        cn.rukkit.network.packet.Packet firstPagePacket = fixture.channel.readOutbound();
        assertNotNull(firstPagePacket);
        String firstPage = readLegacyChatMessage(firstPagePacket);
        assertTrue(firstPage.contains("[0] " + OfficialMap.maps[0]));
        assertTrue(firstPage.contains("[9] " + OfficialMap.maps[9]));
        assertFalse(firstPage.contains("[10] " + OfficialMap.maps[10]));

        fixture.connection.player.isAdmin = true;
        manager.executeChatCommand(
                fixture.connection, "map '" + OfficialMap.maps[0] + "'");
        assertEquals(OfficialMap.maps[0], fixture.connection.currectRoom.config.mapName);
        cn.rukkit.network.packet.Packet mapInfo = fixture.channel.readOutbound();
        assertNotNull(mapInfo);
        assertEquals(cn.rukkit.network.packet.Packet.PACKET_SERVER_INFO, mapInfo.type);
    }

    @Test
    void listsEmptyCustomMapDirectoryWithoutFailingThroughLegacyConnection()
            throws Exception {
        installLegacyCommands();
        LegacyConnectionFixture fixture = newLegacyConnection();

        Rukkit.getCommandManager().executeChatCommand(fixture.connection, "cmaps");
        cn.rukkit.network.packet.Packet messagePacket = fixture.channel.readOutbound();
        assertNotNull(messagePacket);
        assertTrue(readLegacyChatMessage(messagePacket).startsWith("- Help -  Page 1"));
    }

    @Test
    void qcAcceptsOptionalPrefixThroughLegacyConnection() throws Exception {
        installLegacyCommands();
        LegacyConnectionFixture fixture = newLegacyConnection();
        byte[] expected = cn.rukkit.network.packet.Packet.chat(
                "SERVER",
                "Rukkit Server v" + cn.rukkit.util.VersionUtil.getVersion()
                        + "\nRukkit Plugin API v" + Rukkit.PLUGIN_API_VERSION,
                -1).bytes;

        for (String prefix : new String[]{"", ".", "-"}) {
            Rukkit.getCommandManager().executeChatCommand(
                    fixture.connection, "qc " + prefix + "version");
            cn.rukkit.network.packet.Packet actual = fixture.channel.readOutbound();
            assertNotNull(actual);
            assertArrayEquals(expected, actual.bytes);
        }
    }

    @Test
    void executesLowRiskRoomCommandsThroughMigratedConnection() throws Exception {
        ServerConnectionFixture fixture = newServerConnection();
        CommandManager manager = Rukkit.getCommandManager();

        manager.executeChatCommand(fixture.connection, "self_team 3");
        assertEquals(2, fixture.connection.player.team);

        manager.executeChatCommand(fixture.connection, "t hello team");
        Packet teamMessage = fixture.channel.readOutbound();
        assertNotNull(teamMessage);
        assertEquals(PacketType.SEND_CHAT, teamMessage.type);
        assertTrue(readChatMessage(teamMessage).endsWith("hello team"));

        manager.executeChatCommand(fixture.connection, "list");
        Packet playerList = readServerMessage(fixture.channel);
        assertTrue(readChatMessage(playerList).contains("Alice (Team 2)"));

        manager.executeChatCommand(fixture.connection, "maping 10 20");
        Packet ping = fixture.channel.readOutbound();
        assertNotNull(ping);
        assertEquals(PacketType.TICK, ping.type);

        manager.executeChatCommand(fixture.connection, "chksum");
        Packet checksum = fixture.channel.readOutbound();
        assertNotNull(checksum);
        assertEquals(PacketType.SYNC_CHECKSUM, checksum.type);

        manager.executeChatCommand(fixture.connection, "surrender");
        assertTrue(fixture.connection.player.isSurrounded);
        Packet surrenderAction = fixture.channel.readOutbound();
        Packet surrenderMessage = fixture.channel.readOutbound();
        assertNotNull(surrenderAction);
        assertNotNull(surrenderMessage);
        assertEquals(PacketType.TICK, surrenderAction.type);
        assertEquals(PacketType.SEND_CHAT, surrenderMessage.type);
    }

    @Test
    void executesCoreRoomConfigurationCommandsThroughMigratedConnection() throws Exception {
        ServerConnectionFixture fixture = newServerConnection();
        fixture.connection.player.isAdmin = true;
        CommandManager manager = Rukkit.getCommandManager();

        manager.executeChatCommand(fixture.connection, "fog los");
        assertEquals(2, fixture.connection.currectRoom.config.fogType);
        assertServerInfoPair(fixture.channel);

        manager.executeChatCommand(fixture.connection, "startingunits 7");
        assertEquals(7, fixture.connection.currectRoom.config.startingUnits);
        assertServerInfoPair(fixture.channel);

        manager.executeChatCommand(fixture.connection, "income 2.5");
        assertEquals(2.5f, fixture.connection.currectRoom.config.income);
        assertServerInfoPair(fixture.channel);

        manager.executeChatCommand(fixture.connection, "credits 1234");
        assertEquals(1234, fixture.connection.currectRoom.config.credits);
        assertServerInfoPair(fixture.channel);

        manager.executeChatCommand(fixture.connection, "nukes true");
        assertEquals(false, fixture.connection.currectRoom.config.disableNuke);
        assertServerInfoPair(fixture.channel);

        fixture.connection.currectRoom.config.sharedControl = true;
        manager.executeChatCommand(fixture.connection, "share on");
        assertTrue(fixture.connection.player.isSharingControl);
        Packet shareMessage = fixture.channel.readOutbound();
        assertNotNull(shareMessage);
        assertEquals(PacketType.SEND_CHAT, shareMessage.type);

        manager.executeChatCommand(fixture.connection, "map 1");
        assertEquals(0, fixture.connection.currectRoom.config.mapType);
        assertServerInfoPair(fixture.channel);
    }

    @Test
    void shareMatchesOriginalToggleAndPermissionBehaviorThroughMigratedConnection()
            throws Exception {
        ServerConnectionFixture fixture = newServerConnection();
        CommandManager manager = Rukkit.getCommandManager();

        manager.executeChatCommand(fixture.connection, "share on");
        assertFalse(fixture.connection.player.isSharingControl);
        assertEquals("[Shared control is not enabled in this game]",
                readChatMessage(readServerMessage(fixture.channel)));

        fixture.connection.currectRoom.config.sharedControl = true;
        manager.executeChatCommand(fixture.connection, "share on");
        assertTrue(fixture.connection.player.isSharingControl);
        assertEquals("[shared control now on for Alice]",
                readChatMessage(readServerMessage(fixture.channel)));

        manager.executeChatCommand(fixture.connection, "share on");
        assertEquals("[shared control already on for Alice]",
                readChatMessage(readServerMessage(fixture.channel)));

        manager.executeChatCommand(fixture.connection, "share off");
        assertFalse(fixture.connection.player.isSharingControl);
        assertEquals("[shared control now off for Alice]",
                readChatMessage(readServerMessage(fixture.channel)));

        manager.executeChatCommand(fixture.connection, "share maybe");
        assertFalse(fixture.connection.player.isSharingControl);
        assertEquals("[Expected true or false]",
                readChatMessage(readServerMessage(fixture.channel)));
    }

    @Test
    void shareMatchesOriginalToggleBehaviorThroughLegacyConnection() throws Exception {
        installLegacyCommands();
        LegacyConnectionFixture fixture = newLegacyConnection();
        fixture.connection.currectRoom.config.sharedControl = true;
        CommandManager manager = Rukkit.getCommandManager();

        manager.executeChatCommand(fixture.connection, "share true");
        assertTrue(fixture.connection.player.isSharingControl);
        assertEquals("[shared control now on for Alice]",
                readLegacyChatMessage(fixture.channel.readOutbound()));

        manager.executeChatCommand(fixture.connection, "share false");
        assertFalse(fixture.connection.player.isSharingControl);
        assertEquals("[shared control now off for Alice]",
                readLegacyChatMessage(fixture.channel.readOutbound()));
    }

    private ServerConnectionFixture newServerConnection() {
        ServerConnectionHandler handler = new ServerConnectionHandler(
                new ServerPacketHandlerManager());
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        serverHandlers.add(handler);
        channels.add(channel);

        ServerRoom room = new ServerRoom(0);
        ServerRoomConnection connection = new ServerRoomConnection(handler, room);
        connection.player = new NetworkPlayer(connection);
        connection.player.name = "Alice";
        connection.player.uuid = "simple-command-test";
        room.connectionManager.add(connection);
        handler.setConn(connection);
        handler.setState(ConnectionState.IN_ROOM);
        return new ServerConnectionFixture(channel, connection);
    }

    private LegacyConnectionFixture newLegacyConnection() {
        ConnectionHandler handler = new ConnectionHandler();
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        legacyHandlers.add(handler);
        channels.add(channel);

        NetworkRoom room = new NetworkRoom(0);
        RoomConnection connection = new RoomConnection(handler, room);
        connection.player = new NetworkPlayer(connection);
        connection.player.name = "Alice";
        connection.player.uuid = "legacy-simple-command-test";
        room.connectionManager.add(connection);
        handler.setState(ConnectionState.IN_ROOM);
        return new LegacyConnectionFixture(channel, connection);
    }

    private static Packet readServerMessage(EmbeddedChannel channel) {
        Packet packet = channel.readOutbound();
        assertNotNull(packet);
        assertEquals(PacketType.SEND_CHAT, packet.type);
        return packet;
    }

    private static void assertServerInfoPair(EmbeddedChannel channel) {
        Packet broadcast = channel.readOutbound();
        Packet admin = channel.readOutbound();
        assertNotNull(broadcast);
        assertNotNull(admin);
        assertEquals(PacketType.SERVER_INFO, broadcast.type);
        assertEquals(PacketType.SERVER_INFO, admin.type);
    }

    private static String readChatMessage(Packet packet) throws IOException {
        return new GameInputStream(packet).readString();
    }

    private static String readLegacyChatMessage(cn.rukkit.network.packet.Packet packet)
            throws IOException {
        return new GameInputStream(packet.bytes).readString();
    }

    private void unregisterPluginListeners() throws ReflectiveOperationException {
        Field field = RukkitPlugin.class.getDeclaredField("listeners");
        field.setAccessible(true);
        PluginManager pluginManager = Rukkit.getPluginManager();
        if (pluginManager != null) {
            for (RukkitPlugin plugin : new RukkitPlugin[]{coreCommandPlugin, legacyCommandPlugin}) {
                if (plugin == null) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                List<cn.rukkit.event.EventListener> listeners =
                        (List<cn.rukkit.event.EventListener>) field.get(plugin);
                for (cn.rukkit.event.EventListener listener : listeners) {
                    pluginManager.unregisterEventListener(listener);
                }
            }
        }
    }

    private void installLegacyCommands() {
        try {
            setStatic("commandManager", new CommandManager());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        legacyCommandPlugin = new CommandPlugin();
        legacyCommandPlugin.onLoad();
        assertNull(Rukkit.getCommandManager().fetchCommand("version").getContextListener());
    }

    private static Object setStatic(String name, Object value) throws ReflectiveOperationException {
        Field field = Rukkit.class.getDeclaredField(name);
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, value);
        return previous;
    }

    private record ServerConnectionFixture(EmbeddedChannel channel,
                                           ServerRoomConnection connection) {
    }

    private record LegacyConnectionFixture(EmbeddedChannel channel,
                                           RoomConnection connection) {
    }
}
