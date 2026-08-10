/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.core;

import cn.rukkit.Rukkit;
import cn.rukkit.network.core.handler.ServerConnectionHandler;
import cn.rukkit.network.core.handler.ServerPacketHandlerManager;
import cn.rukkit.network.core.packet.PacketDecoder;
import cn.rukkit.network.core.packet.PacketEncoder;
import cn.rukkit.network.room.ServerGlobalConnectionManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Netty entry point for the migrated packet and room runtime.
 *
 * <p>The server deliberately mirrors the legacy server's blocking
 * {@link #action(long)} contract so it can be submitted to the existing
 * {@code ThreadManager}. The runtime owns only core-layer objects; legacy
 * packet handlers are never installed in this pipeline.</p>
 */
public final class CoreRoomGameServer {
    private static final Logger LOG = LoggerFactory.getLogger("CoreGameServer");

    private final int port;
    private final ServerPacketHandlerManager packetHandlerManager;
    private final ServerGlobalConnectionManager globalConnectionManager;
    private final CountDownLatch bindCompleted = new CountDownLatch(1);

    private volatile NioEventLoopGroup bossGroup;
    private volatile NioEventLoopGroup workerGroup;
    private volatile ChannelFuture serverFuture;
    private volatile Throwable startFailure;

    public CoreRoomGameServer(int port,
                              ServerPacketHandlerManager packetHandlerManager,
                              ServerGlobalConnectionManager globalConnectionManager) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        this.port = port;
        this.packetHandlerManager = Objects.requireNonNull(
                packetHandlerManager, "packetHandlerManager must not be null");
        this.globalConnectionManager = Objects.requireNonNull(
                globalConnectionManager, "globalConnectionManager must not be null");
    }

    /**
     * Starts listening and blocks until the listening channel is closed.
     */
    public void action(final long time) throws InterruptedException {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new LoggingHandler(LOG.getName(), LogLevel.DEBUG))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) {
                            channel.pipeline()
                                    .addLast("packet-decoder", new PacketDecoder())
                                    .addLast("packet-encoder", new PacketEncoder())
                                    .addLast("connection", new ServerConnectionHandler(
                                            packetHandlerManager,
                                            globalConnectionManager::discard));
                        }
                    });

            serverFuture = bootstrap.bind(port).sync();
            bindCompleted.countDown();
            LOG.info("Done! ({}ms), core network listening on {}",
                    System.currentTimeMillis() - time, getBoundPort());
            Rukkit.setStarted(true);
            serverFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            startFailure = e;
            bindCompleted.countDown();
            throw e;
        } catch (Throwable e) {
            startFailure = e;
            bindCompleted.countDown();
            LOG.error("Unable to start core game server", e);
        } finally {
            Rukkit.setStarted(false);
            shutdownGroups();
        }
    }

    /**
     * Waits until bind succeeds or fails. This is useful for startup
     * coordination and loopback smoke tests without exposing Netty internals.
     */
    public boolean awaitStarted(long timeout, TimeUnit unit) throws InterruptedException {
        if (!bindCompleted.await(timeout, unit)) {
            return false;
        }
        return startFailure == null && getBoundPort() >= 0;
    }

    public boolean isRunning() {
        ChannelFuture future = serverFuture;
        return future != null && future.channel().isOpen();
    }

    /** Returns the actual bound port, or -1 before a successful bind. */
    public int getBoundPort() {
        ChannelFuture future = serverFuture;
        if (future == null || future.channel().localAddress() == null) {
            return -1;
        }
        if (future.channel().localAddress() instanceof InetSocketAddress address) {
            return address.getPort();
        }
        return -1;
    }

    public Throwable getStartFailure() {
        return startFailure;
    }

    public void stopServer() {
        ChannelFuture future = serverFuture;
        if (future != null) {
            future.channel().close();
        }
        shutdownGroups();
    }

    private synchronized void shutdownGroups() {
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            bossGroup = null;
        }
    }
}
