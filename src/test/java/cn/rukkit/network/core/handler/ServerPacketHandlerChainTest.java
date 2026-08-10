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
import cn.rukkit.network.ConnectionState;
import cn.rukkit.network.core.packet.Packet;
import cn.rukkit.network.room.ServerRoom;
import cn.rukkit.network.room.ServerRoomConnection;
import cn.rukkit.service.ThreadManager;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class ServerPacketHandlerChainTest {
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
    void dispatchesCorePacketAndAllowsStateTransition() {
        ServerPacketHandlerManager manager = new ServerPacketHandlerManager();
        AtomicReference<ServerPacketContext> received = new AtomicReference<>();
        manager.register(new ServerPacketHandler() {
            @Override
            public int getType() {
                return 7;
            }

            @Override
            public List<ConnectionState> getAllowedStates() {
                return List.of(ConnectionState.CONNECTED);
            }

            @Override
            public void handle(ServerPacketContext context, Packet packet) {
                received.set(context);
                context.transitionTo(ConnectionState.PRE_REGISTERED);
            }
        });

        ServerConnectionHandler handler = newHandler(manager);
        EmbeddedChannel channel = channels.get(channels.size() - 1);

        assertFalse(channel.writeInbound(new Packet(7, new byte[] {1, 2, 3})));
        assertSame(handler, received.get().handler());
        assertEquals(ConnectionState.PRE_REGISTERED, handler.getState());
    }

    @Test
    void closesConnectionWhenPacketStateIsNotAllowed() {
        ServerPacketHandlerManager manager = new ServerPacketHandlerManager();
        manager.register(new ServerPacketHandler() {
            @Override
            public int getType() {
                return 8;
            }

            @Override
            public List<ConnectionState> getAllowedStates() {
                return List.of(ConnectionState.PRE_REGISTERED);
            }

            @Override
            public void handle(ServerPacketContext context, Packet packet) {
                throw new AssertionError("blocked packet must not reach handler");
            }
        });

        ServerConnectionHandler handler = newHandler(manager);
        EmbeddedChannel channel = channels.get(channels.size() - 1);

        channel.writeInbound(new Packet(8, new byte[] {9}));

        assertFalse(channel.isOpen());
        assertEquals(ConnectionState.DISCONNECTED, handler.getState());
    }

    @Test
    void contextBindingMovesRoomConnectionIntoHandler() {
        ServerPacketHandlerManager manager = new ServerPacketHandlerManager();
        ServerConnectionHandler handler = newHandler(manager);
        ServerRoom room = new ServerRoom(0);
        ServerRoomConnection connection = new ServerRoomConnection(handler, room);
        connection.player = new NetworkPlayer(connection);

        ServerPacketContext context = new ServerPacketContext(handler.ctx, null, handler);
        context.bindConnection(connection);

        assertSame(connection, handler.getConn());
        assertSame(room, handler.getCurrentRoom());
    }

    @Test
    void disconnectInvokesGlobalCleanupCallback() {
        ServerPacketHandlerManager manager = new ServerPacketHandlerManager();
        AtomicReference<ServerRoomConnection> discarded = new AtomicReference<>();
        ServerConnectionHandler handler = new ServerConnectionHandler(manager, discarded::set);
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        handlers.add(handler);
        channels.add(channel);

        ServerRoom room = new ServerRoom(0);
        ServerRoomConnection connection = new ServerRoomConnection(handler, room);
        connection.player = new NetworkPlayer(connection);
        room.connectionManager.add(connection);
        handler.setConn(connection);

        channel.close();

        assertSame(connection, discarded.get());
        assertFalse(room.connectionManager.contains(connection));
    }

    private ServerConnectionHandler newHandler(ServerPacketHandlerManager manager) {
        ServerConnectionHandler handler = new ServerConnectionHandler(manager);
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        handlers.add(handler);
        channels.add(channel);
        return handler;
    }

    private static Object setStatic(String name, Object value) throws ReflectiveOperationException {
        Field field = Rukkit.class.getDeclaredField(name);
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, value);
        return previous;
    }
}
