/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.command;

import cn.rukkit.Rukkit;
import cn.rukkit.config.RoundConfig;
import cn.rukkit.config.RukkitConfig;
import cn.rukkit.game.NetworkPlayer;
import cn.rukkit.network.ConnectionState;
import cn.rukkit.network.core.handler.ServerConnectionHandler;
import cn.rukkit.network.core.handler.ServerPacketHandlerManager;
import cn.rukkit.network.core.packet.Packet;
import cn.rukkit.network.core.packet.PacketType;
import cn.rukkit.network.room.ServerRoom;
import cn.rukkit.network.room.ServerRoomConnection;
import cn.rukkit.plugin.PluginConfig;
import cn.rukkit.plugin.RukkitPlugin;
import cn.rukkit.service.ThreadManager;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CommandManagerServerConnectionTest {
    private final List<EmbeddedChannel> channels = new ArrayList<>();
    private final List<ServerConnectionHandler> handlers = new ArrayList<>();
    private Object previousConfig;
    private Object previousRound;
    private Object previousThreadManager;
    private ThreadManager testThreadManager;

    @BeforeEach
    void installTestConfiguration() throws ReflectiveOperationException {
        RukkitConfig config = new RukkitConfig();
        config.maxPlayer = 2;
        previousConfig = setStatic("config", config);
        previousRound = setStatic("round", new RoundConfig());
        testThreadManager = new ThreadManager(1);
        previousThreadManager = setStatic("threadManager", testThreadManager);
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
    }

    @Test
    void executesServerCommandWithArgumentsAndEchoesIt() throws Exception {
        CommandManager manager = new CommandManager();
        TestPlugin plugin = new TestPlugin();
        ChatCommand command = new ChatCommand("echo", "", 1,
                (connection, args) -> false, plugin);
        command.setContextListener((context, args) -> {
            assertEquals("hello world", args[0]);
            context.sendServerMessage("executed");
            return true;
        });
        manager.registerCommand(command);

        ConnectionFixture fixture = newConnection();
        manager.executeChatCommand(fixture.connection, "echo hello world");

        Packet serverMessage = fixture.channel.readOutbound();
        Packet echo = fixture.channel.readOutbound();
        assertNotNull(serverMessage);
        assertNotNull(echo);
        assertEquals(PacketType.SEND_CHAT, serverMessage.type);
        assertEquals(PacketType.SEND_CHAT, echo.type);
        assertArrayEquals(
                cn.rukkit.network.core.packet.UniversalPacket.chat("SERVER", "executed", -1).bytes,
                serverMessage.bytes);
        assertArrayEquals(
                cn.rukkit.network.core.packet.UniversalPacket.chat("Alice", "-echo hello world", 0).bytes,
                echo.bytes);
    }

    @Test
    void appliesPermissionCheckBeforeContextListener() throws Exception {
        CommandManager manager = new CommandManager();
        TestPlugin plugin = new TestPlugin();
        ChatCommand command = new ChatCommand("admin", "", 0,
                (connection, args) -> false, plugin, true);
        command.setContextListener((context, args) -> {
            throw new AssertionError("permission denied command must not execute");
        });
        manager.registerCommand(command);

        ConnectionFixture fixture = newConnection();
        manager.executeChatCommand(fixture.connection, "admin");

        Packet response = fixture.channel.readOutbound();
        assertNotNull(response);
        assertEquals(PacketType.SEND_CHAT, response.type);
        assertArrayEquals(
                cn.rukkit.network.core.packet.UniversalPacket.chat(
                        "SERVER", cn.rukkit.util.LangUtil.getString("chat.privDenied"), -1).bytes,
                response.bytes);
    }

    private ConnectionFixture newConnection() {
        ServerConnectionHandler handler = new ServerConnectionHandler(
                new ServerPacketHandlerManager());
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        handlers.add(handler);
        channels.add(channel);

        ServerRoom room = new ServerRoom(0);
        ServerRoomConnection connection = new ServerRoomConnection(handler, room);
        connection.player = new NetworkPlayer(connection);
        connection.player.name = "Alice";
        connection.player.uuid = "uuid-command-test";
        room.connectionManager.add(connection);
        handler.setConn(connection);
        handler.setState(ConnectionState.IN_ROOM);
        return new ConnectionFixture(channel, connection);
    }

    private static Object setStatic(String name, Object value) throws ReflectiveOperationException {
        Field field = Rukkit.class.getDeclaredField(name);
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, value);
        return previous;
    }

    private record ConnectionFixture(EmbeddedChannel channel,
                                     ServerRoomConnection connection) {
    }

    private static final class TestPlugin extends RukkitPlugin {
        private TestPlugin() {
            config = new PluginConfig();
            config.name = "command-test";
            config.id = "command-test";
        }

        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }

        @Override
        public void onStart() {
        }

        @Override
        public void onDone() {
        }
    }
}
