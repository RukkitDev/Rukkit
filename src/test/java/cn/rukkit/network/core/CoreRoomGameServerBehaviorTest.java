/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 */

package cn.rukkit.network.core;

import cn.rukkit.Rukkit;
import cn.rukkit.config.RoundConfig;
import cn.rukkit.config.RukkitConfig;
import cn.rukkit.game.mod.ModManager;
import cn.rukkit.event.player.PlayerJoinEvent;
import cn.rukkit.game.NetworkPlayer;
import cn.rukkit.network.ConnectionState;
import cn.rukkit.network.core.handler.ServerPacketHandlerManager;
import cn.rukkit.network.core.packet.Packet;
import cn.rukkit.network.core.packet.PacketDecoder;
import cn.rukkit.network.core.packet.PacketEncoder;
import cn.rukkit.network.core.packet.PacketType;
import cn.rukkit.network.room.ServerGlobalConnectionManager;
import cn.rukkit.network.room.ServerRoom;
import cn.rukkit.network.room.ServerRoomConnection;
import cn.rukkit.network.room.ServerRoomManager;
import cn.rukkit.service.ThreadManager;
import cn.rukkit.plugin.internal.BasePlugin;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreRoomGameServerBehaviorTest {
    private Object previousConfig;
    private Object previousRound;
    private Object previousThreadManager;
    private Object previousModManager;
    private Object previousStarted;
    private ThreadManager threadManager;
    private ServerRoomManager roomManager;
    private ServerGlobalConnectionManager globalConnectionManager;
    private CoreRoomGameServer server;
    private Thread serverThread;
    private NioEventLoopGroup clientGroup;
    private Channel clientChannel;

    @BeforeEach
    void installTestRuntime() throws ReflectiveOperationException {
        RukkitConfig config = new RukkitConfig();
        config.maxRoom = 2;
        config.maxPlayer = 1;
        config.serverPort = 0;
        config.UUID = "core-smoke-server";
        previousConfig = setStatic("config", config);
        previousRound = setStatic("round", new RoundConfig());
        previousModManager = setStatic("modManager", new ModManager());
        previousStarted = setStatic("isStarted", false);

        threadManager = new ThreadManager(2);
        previousThreadManager = setStatic("threadManager", threadManager);

        roomManager = new ServerRoomManager(Rukkit.getRoundConfig(), 2);
        globalConnectionManager =
                new ServerGlobalConnectionManager(roomManager);
        ServerPacketHandlerManager handlerManager = new ServerPacketHandlerManager();
        handlerManager.registerInternalHandler(roomManager, globalConnectionManager);
        server = new CoreRoomGameServer(0, handlerManager, globalConnectionManager);
    }

    @AfterEach
    void stopTestRuntime() throws Exception {
        if (clientChannel != null) {
            clientChannel.close().syncUninterruptibly();
        }
        if (clientGroup != null) {
            clientGroup.shutdownGracefully().syncUninterruptibly();
        }
        if (server != null) {
            server.stopServer();
        }
        if (serverThread != null) {
            serverThread.join(5000);
        }
        threadManager.shutdown();

        setStatic("config", previousConfig);
        setStatic("round", previousRound);
        setStatic("threadManager", previousThreadManager);
        setStatic("modManager", previousModManager);
        setStatic("isStarted", previousStarted);
    }

    @Test
    void startsCoreListenerAndCompletesPreRegistrationHandshake() throws Exception {
        List<Packet> received = new ArrayList<>();
        CountDownLatch handshakePackets = new CountDownLatch(2);

        serverThread = new Thread(() -> {
            try {
                server.action(System.currentTimeMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "core-server-smoke");
        serverThread.start();

        assertTrue(server.awaitStarted(5, TimeUnit.SECONDS),
                () -> "core server failed to start: " + server.getStartFailure());
        assertTrue(server.isRunning());
        assertTrue(server.getBoundPort() > 0);

        clientGroup = new NioEventLoopGroup(1);
        clientChannel = new Bootstrap()
                .group(clientGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        channel.pipeline()
                                .addLast("packet-decoder", new PacketDecoder())
                                .addLast("packet-encoder", new PacketEncoder())
                                .addLast("collector", new SimpleChannelInboundHandler<Packet>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext context,
                                                                 Packet packet) {
                                        received.add(packet);
                                        handshakePackets.countDown();
                                    }
                                });
                    }
                })
                .connect("127.0.0.1", server.getBoundPort())
                .sync()
                .channel();

        clientChannel.writeAndFlush(new Packet(PacketType.PREREGISTER_CONNECTION, new byte[0]))
                .sync();

        assertTrue(handshakePackets.await(5, TimeUnit.SECONDS));
        assertEquals(2, received.size());
        assertEquals(PacketType.REGISTER_CONNECTION, received.get(0).type);
        assertEquals(PacketType.SEND_CHAT, received.get(1).type);
        assertNotNull(received.get(0).bytes);
        assertNotNull(received.get(1).bytes);
    }

    @Test
    void completesTcpPlayerLifecycleChatAndDisconnect() throws Exception {
        List<Packet> received = new CopyOnWriteArrayList<>();
        CountDownLatch handshakePackets = new CountDownLatch(2);

        serverThread = new Thread(() -> {
            try {
                server.action(System.currentTimeMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "core-server-lifecycle");
        serverThread.start();

        assertTrue(server.awaitStarted(5, TimeUnit.SECONDS),
                () -> "core server failed to start: " + server.getStartFailure());

        clientGroup = new NioEventLoopGroup(1);
        clientChannel = new Bootstrap()
                .group(clientGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        channel.pipeline()
                                .addLast("packet-decoder", new PacketDecoder())
                                .addLast("packet-encoder", new PacketEncoder())
                                .addLast("collector", new SimpleChannelInboundHandler<Packet>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext context,
                                                                 Packet packet) {
                                        received.add(packet);
                                        handshakePackets.countDown();
                                    }
                                });
                    }
                })
                .connect("127.0.0.1", server.getBoundPort())
                .sync()
                .channel();

        clientChannel.writeAndFlush(new Packet(PacketType.PREREGISTER_CONNECTION, new byte[0]))
                .sync();
        assertTrue(handshakePackets.await(5, TimeUnit.SECONDS));

        clientChannel.writeAndFlush(playerInfoPacket("Alice", "tcp-player"))
                .sync();
        awaitCondition(() -> globalConnectionManager.size() == 1
                        && roomManager.getRoom(0).connectionManager.size() == 1,
                5, TimeUnit.SECONDS);

        assertEquals(1, roomManager.getRoom(0).playerManager.getPlayerCount());
        assertEquals("Alice", globalConnectionManager.getConnections().get(0).player.name);
        awaitCondition(() -> received.stream().anyMatch(packet ->
                        packet.type == PacketType.TEAM_LIST),
                5, TimeUnit.SECONDS);
        assertTrue(received.stream().anyMatch(packet -> packet.type == PacketType.SERVER_INFO));
        assertTrue(received.stream().anyMatch(packet -> packet.type == PacketType.TEAM_LIST));

        clientChannel.writeAndFlush(chatPacket("hello over tcp")).sync();
        awaitCondition(() -> received.stream().anyMatch(packet ->
                        packet.type == PacketType.SEND_CHAT),
                5, TimeUnit.SECONDS);

        clientChannel.writeAndFlush(disconnectPacket("client requested disconnect")).sync();
        awaitCondition(() -> globalConnectionManager.size() == 0
                        && roomManager.getRoom(0).connectionManager.size() == 0,
                5, TimeUnit.SECONDS);

        assertEquals(0, roomManager.getRoom(0).playerManager.getPlayerCount());
        assertNull(globalConnectionManager.getPlayerByUUID("tcp-player"));
    }

    @Test
    void basePluginEmitsJoinMessageForCoreRoomPlayer() {
        cn.rukkit.network.core.handler.ServerConnectionHandler handler =
                new cn.rukkit.network.core.handler.ServerConnectionHandler(
                        new ServerPacketHandlerManager());
        io.netty.channel.embedded.EmbeddedChannel channel =
                new io.netty.channel.embedded.EmbeddedChannel(handler);
        ServerRoom room = roomManager.getRoom(0);
        cn.rukkit.network.room.ServerRoomConnection connection =
                new cn.rukkit.network.room.ServerRoomConnection(handler, room);
        NetworkPlayer player = new NetworkPlayer(connection);
        player.name = "CoreAlice";
        connection.player = player;
        room.connectionManager.add(connection);

        new BasePlugin().onPlayerJoinTip(new PlayerJoinEvent(player));

        Packet message = channel.readOutbound();
        assertNotNull(message);
        assertEquals(PacketType.SEND_CHAT, message.type);
        channel.finishAndReleaseAll();
    }

    @Test
    void roomIsGamingImmediatelyAfterStartAndStartIsIdempotent()
            throws ReflectiveOperationException {
        ServerRoom room = roomManager.getRoom(0);

        room.startGame();
        try {
            assertTrue(room.isGaming());
            Field taskField = ServerRoom.class.getDeclaredField("gameTaskFuture");
            taskField.setAccessible(true);
            Object firstTask = taskField.get(room);

            room.startGame();

            assertSame(firstTask, taskField.get(room));
        } finally {
            room.stopGame();
        }
    }

    @Test
    void reconnectsOverTcpAndReusesDisconnectedPlayer() throws Exception {
        TestServerRoom reconnectRoom = new TestServerRoom(0);
        roomManager.roomList.set(0, reconnectRoom);

        List<Packet> firstReceived = new CopyOnWriteArrayList<>();
        CountDownLatch firstHandshake = new CountDownLatch(2);
        serverThread = new Thread(() -> {
            try {
                server.action(System.currentTimeMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "core-server-reconnect");
        serverThread.start();
        assertTrue(server.awaitStarted(5, TimeUnit.SECONDS),
                () -> "core server failed to start: " + server.getStartFailure());

        clientGroup = new NioEventLoopGroup(1);
        clientChannel = new Bootstrap()
                .group(clientGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        channel.pipeline()
                                .addLast("packet-decoder", new PacketDecoder())
                                .addLast("packet-encoder", new PacketEncoder())
                                .addLast("collector", new SimpleChannelInboundHandler<Packet>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext context,
                                                                 Packet packet) {
                                        firstReceived.add(packet);
                                        firstHandshake.countDown();
                                    }
                                });
                    }
                })
                .connect("127.0.0.1", server.getBoundPort())
                .sync()
                .channel();
        clientChannel.writeAndFlush(new Packet(PacketType.PREREGISTER_CONNECTION, new byte[0]))
                .sync();
        assertTrue(firstHandshake.await(5, TimeUnit.SECONDS));
        clientChannel.writeAndFlush(playerInfoPacket("Alice", "tcp-reconnect"))
                .sync();
        awaitCondition(() -> globalConnectionManager.size() == 1
                        && reconnectRoom.connectionManager.size() == 1,
                5, TimeUnit.SECONDS);

        NetworkPlayer originalPlayer = globalConnectionManager.getConnections().get(0).player;
        reconnectRoom.currentStep = 10;
        clientChannel.close().syncUninterruptibly();
        clientChannel = null;
        awaitCondition(() -> globalConnectionManager.size() == 0
                        && reconnectRoom.connectionManager.size() == 0
                        && originalPlayer.isDisconnected,
                5, TimeUnit.SECONDS);

        List<Packet> secondReceived = new CopyOnWriteArrayList<>();
        CountDownLatch secondHandshake = new CountDownLatch(2);
        clientChannel = new Bootstrap()
                .group(clientGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        channel.pipeline()
                                .addLast("packet-decoder", new PacketDecoder())
                                .addLast("packet-encoder", new PacketEncoder())
                                .addLast("collector", new SimpleChannelInboundHandler<Packet>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext context,
                                                                 Packet packet) {
                                        secondReceived.add(packet);
                                        secondHandshake.countDown();
                                    }
                                });
                    }
                })
                .connect("127.0.0.1", server.getBoundPort())
                .sync()
                .channel();
        clientChannel.writeAndFlush(new Packet(PacketType.PREREGISTER_CONNECTION, new byte[0]))
                .sync();
        assertTrue(secondHandshake.await(5, TimeUnit.SECONDS));
        clientChannel.writeAndFlush(playerInfoPacket("Alice-Reconnected", "tcp-reconnect"))
                .sync();

        awaitCondition(() -> globalConnectionManager.size() == 1
                        && reconnectRoom.connectionManager.size() == 1,
                5, TimeUnit.SECONDS);
        ServerRoomConnection reconnected = reconnectRoom.connectionManager.getConnections().get(0);
        assertSame(originalPlayer, reconnected.player);
        assertEquals("Alice-Reconnected", reconnected.player.name);
        assertFalse(reconnected.player.isDisconnected);
        assertEquals(ConnectionState.IN_GAME,
                reconnected.handler.getState());
        awaitCondition(() -> secondReceived.stream().anyMatch(
                        packet -> packet.type == PacketType.START_GAME),
                5, TimeUnit.SECONDS);
        assertTrue(secondReceived.stream().anyMatch(packet -> packet.type == PacketType.START_GAME));
    }

    @Test
    void routesTwoPlayersToIndependentRoomsAndStartsGamesIndependently() throws Exception {
        Rukkit.getConfig().singlePlayerMode = true;
        serverThread = new Thread(() -> {
            try {
                server.action(System.currentTimeMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "core-server-multi-room");
        serverThread.start();
        assertTrue(server.awaitStarted(5, TimeUnit.SECONDS),
                () -> "core server failed to start: " + server.getStartFailure());

        clientGroup = new NioEventLoopGroup(2);
        ClientFixture first = connectClient("Alice", "multi-room-first");
        clientChannel = first.channel;
        ClientFixture second = connectClient("Bob", "multi-room-second");

        awaitCondition(() -> globalConnectionManager.getConnections().size() == 2
                        && roomManager.getRoom(0).connectionManager.getConnections().size() == 1
                        && roomManager.getRoom(1).connectionManager.getConnections().size() == 1,
                5, TimeUnit.SECONDS);

        ServerRoomConnection firstConnection = findConnection("multi-room-first");
        ServerRoomConnection secondConnection = findConnection("multi-room-second");
        assertNotNull(firstConnection);
        assertNotNull(secondConnection);
        assertEquals(0, firstConnection.currectRoom.roomId);
        assertEquals(1, secondConnection.currectRoom.roomId);
        assertEquals(1, roomManager.getRoom(0).playerManager.getPlayerCount());
        assertEquals(1, roomManager.getRoom(1).playerManager.getPlayerCount());

        roomManager.getRoom(0).startGame();
        awaitCondition(() -> hasPacket(first.received, PacketType.START_GAME),
                5, TimeUnit.SECONDS);
        assertFalse(hasPacket(second.received, PacketType.START_GAME));

        roomManager.getRoom(1).startGame();
        awaitCondition(() -> hasPacket(second.received, PacketType.START_GAME),
                5, TimeUnit.SECONDS);

        roomManager.getRoom(0).stopGame();
        roomManager.getRoom(1).stopGame();
        second.channel.close().syncUninterruptibly();
    }

    private ClientFixture connectClient(String name, String uuid) throws Exception {
        List<Packet> received = new CopyOnWriteArrayList<>();
        CountDownLatch handshakePackets = new CountDownLatch(2);
        Channel channel = new Bootstrap()
                .group(clientGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        channel.pipeline()
                                .addLast("packet-decoder", new PacketDecoder())
                                .addLast("packet-encoder", new PacketEncoder())
                                .addLast("collector", new SimpleChannelInboundHandler<Packet>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext context,
                                                                 Packet packet) {
                                        received.add(packet);
                                        handshakePackets.countDown();
                                    }
                                });
                    }
                })
                .connect("127.0.0.1", server.getBoundPort())
                .sync()
                .channel();
        channel.writeAndFlush(new Packet(PacketType.PREREGISTER_CONNECTION, new byte[0]))
                .sync();
        assertTrue(handshakePackets.await(5, TimeUnit.SECONDS));
        channel.writeAndFlush(playerInfoPacket(name, uuid)).sync();
        return new ClientFixture(channel, received);
    }

    private ServerRoomConnection findConnection(String uuid) {
        return globalConnectionManager.getConnections().stream()
                .filter(connection -> uuid.equals(connection.player.uuid))
                .findFirst()
                .orElse(null);
    }

    private static boolean hasPacket(List<Packet> packets, int packetType) {
        return packets.stream().anyMatch(packet -> packet.type == packetType);
    }

    private static Packet playerInfoPacket(String name, String uuid) throws Exception {
        cn.rukkit.network.io.GameOutputStream output =
                new cn.rukkit.network.io.GameOutputStream();
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

    private static Packet chatPacket(String message) throws Exception {
        cn.rukkit.network.io.GameOutputStream output =
                new cn.rukkit.network.io.GameOutputStream();
        output.writeString(message);
        return output.createPacket(PacketType.ADD_CHAT);
    }

    private static Packet disconnectPacket(String reason) throws Exception {
        cn.rukkit.network.io.GameOutputStream output =
                new cn.rukkit.network.io.GameOutputStream();
        output.writeString(reason);
        return output.createPacket(PacketType.DISCONNECT);
    }

    private static void awaitCondition(Condition condition, long timeout, TimeUnit unit)
            throws Exception {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (!condition.matches()) {
            if (System.nanoTime() >= deadline) {
                throw new AssertionError("condition was not met within " + timeout + " " + unit);
            }
            Thread.sleep(10);
        }
    }

    @FunctionalInterface
    private interface Condition {
        boolean matches();
    }

    private static Object setStatic(String name, Object value)
            throws ReflectiveOperationException {
        Field field = Rukkit.class.getDeclaredField(name);
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, value);
        return previous;
    }

    private static final class TestServerRoom extends ServerRoom {
        private TestServerRoom(int id) {
            super(id);
        }

        @Override
        public void syncGame() {
        }
    }

    private record ClientFixture(Channel channel, List<Packet> received) {
    }
}
