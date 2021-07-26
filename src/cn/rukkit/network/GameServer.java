package cn.rukkit.network;

import cn.rukkit.*;
import cn.rukkit.network.packet.*;
import io.netty.bootstrap.*;
import io.netty.channel.*;
import io.netty.channel.nio.*;
import io.netty.channel.socket.*;
import io.netty.channel.socket.nio.*;
import org.slf4j.*;
import io.netty.handler.logging.*;

public class GameServer
{
	Logger log = LoggerFactory.getLogger(GameServer.class);

	private int port;
	private boolean isGaming = false;
	public GameServer(int port) {
		this.port = port;
	}
	
	public boolean isGaming() {
		return isGaming;
	}
	
	public void action(final long time) throws InterruptedException{
		// 用来接收进来的连接
        EventLoopGroup bossGroup = new NioEventLoopGroup(); 
        // 用来处理已经被接收的连接，一旦bossGroup接收到连接，就会把连接信息注册到workerGroup上
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try{
			ServerBootstrap sbs = new ServerBootstrap();
			sbs.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.option(ChannelOption.SO_BACKLOG, 128)
				.childOption(ChannelOption.SO_KEEPALIVE, true)
				//.handler(new LoggingHandler(log))
				.handler(new LoggingHandler(log.getName(),LogLevel.ERROR))
				.childHandler(new ChannelInitializer<SocketChannel>(){

					@Override
					protected void initChannel(SocketChannel p1) throws Exception
					{
						// TODO: Implement this method
						p1.pipeline().addLast(new PacketDecoder());
						p1.pipeline().addLast(new PacketEncoder()).addLast(new ConnectionHandler());
					}
				});
			//System.out.println("-Server started!");
			new Thread(new Runnable() {
					@Override
					public void run()
					{
						//Rukkit.getCurrentPluginManager().serverDone(Game.this);
						log.info("Done! (" + (System.currentTimeMillis()-time) + "ms)");
						//RukkitConsoleHandler handler = new RukkitConsoleHandler();
						//handler.start();
						//Rukkit.setConsole(handler);
						// TODO: Implement this method
					}
				}).start();
			ChannelFuture cf = sbs.bind(port).sync();
			cf.channel().closeFuture().sync();
		}catch(Exception e) {
			log.error("A error occoured: ", e);
			Rukkit.shutdown(e.getMessage());
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
			return;
		}
		finally{
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
}
