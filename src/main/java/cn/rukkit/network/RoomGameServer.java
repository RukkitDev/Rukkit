/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network;

import cn.rukkit.Rukkit;
import cn.rukkit.network.packet.PacketDecoder;
import cn.rukkit.network.packet.PacketEncoder;
import io.netty.bootstrap.ServerBootstrap;
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

public class RoomGameServer {
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;
    private ChannelFuture serverFuture;

    private Logger log = LoggerFactory.getLogger("GameServer");

    /**
     * Start a Server.
     */
    public void action(final long time) throws InterruptedException {
        // 用来接收进来的连接
        bossGroup = new NioEventLoopGroup();
        // 用来处理已经被接收的连接，一旦bossGroup接收到连接，就会把连接信息注册到workerGroup上
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap sbs = new ServerBootstrap();
            sbs.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    //.handler(new LoggingHandler(log))
                    .handler(new LoggingHandler(log.getName(), LogLevel.DEBUG))
                    .childHandler(new ChannelInitializer<SocketChannel>(){

                        @Override
                        protected void initChannel(SocketChannel p1) throws Exception {
                            // TODO: Implement this method
                            p1.pipeline().addLast(new PacketDecoder());
                            p1.pipeline().addLast(new PacketEncoder()).addLast(new ConnectionHandler());
                        }
                    });
            //System.out.println("-Server started!");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    log.info("Done! (" + (System.currentTimeMillis() - time) + "ms)");
                    Rukkit.setStarted(true);
                    // TODO: Implement this method
                }
            }).start();
            serverFuture = sbs.bind(Rukkit.getConfig().serverPort).sync();
            serverFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("A error occurred: ", e);
            Rukkit.shutdown(e.getMessage());
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            return;
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public void stopServer() {
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
    }
}
