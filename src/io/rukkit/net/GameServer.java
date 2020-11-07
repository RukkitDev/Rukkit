package io.rukkit.net;
import io.netty.bootstrap.*;
import io.netty.channel.*;
import io.netty.channel.nio.*;
import io.netty.channel.socket.*;
import io.netty.channel.socket.nio.*;
import io.netty.handler.logging.*;
import io.rukkit.command.*;
import java.io.*;
import java.util.*;
import io.rukkit.util.*;
import io.rukkit.*;
import io.rukkit.entity.*;


public final class GameServer
{
	
	private int port;
	
	private static Logger log = new Logger("GameServer");
	public static int tickTime = -100;
	public static volatile LinkedList<GameCommand> commandQuere = new LinkedList<GameCommand>();
	//Set a channelGroup
	
	public class GameTickTask extends TimerTask
	{
		
		@Override
		public void run()
		{
			try{
			if(ChannelGroups.size() == 1 && !ServerProperties.singlePlayerMode){
				tickTime = -100;
				new Logger("Game").d("Game stopped!");
				ChannelGroups.broadcast(new Packet().chat("Server", "只剩你一人，自动断线！Only one player in server, Reset!", -1));
				PlayerGroup.reset();
				cancel();
			}
			
			if(ChannelGroups.size() <= 0){
				tickTime = -100;
				new Logger("Game").d("Game stopped!");
				PlayerGroup.reset();
				cancel();
			}
				synchronized(commandQuere){
				if(commandQuere.isEmpty()){
					ChannelGroups.broadcast(new Packet().emptyCommand(tickTime));
				}else{
					ChannelGroups.broadcast(new Packet().emptyCommand(tickTime));
					while(!commandQuere.isEmpty()){
						GameCommand cmd = commandQuere.removeLast();
						ChannelGroups.broadcast(new Packet().gameCommand(tickTime, cmd));
					}
				}
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			// TODO: Implement this method
			tickTime += 10;
		}
		
	}
	
	private Timer gameTickTask = new Timer();
	
	public GameServer(int port){
		this.port = port;
	}
	
	public void startGame(){
		try
		{
			tickTime = 0;
			ChannelGroups.broadcast(new Packet().gameStart());
			if(ServerProperties.sharedControl){
				for(Player p : PlayerGroup.getPlayers()){
					try{
					p.isSharingControl = true;
					}catch(NullPointerException e){continue;}
				}
			}
			gameTickTask.schedule(new GameTickTask(), 0, 200);
			
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static boolean isGaming(){
		return tickTime >= 0;
	}
	
	public void action(long time) throws InterruptedException{
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
			.handler(new LoggingHandler(LogLevel.INFO))
				.childHandler(new ChannelInitializer<SocketChannel>(){

					@Override
					protected void initChannel(SocketChannel p1) throws Exception
					{
						// TODO: Implement this method
						p1.pipeline().addLast(new PacketDecoder());
						p1.pipeline().addLast(new PacketEncoder()).addLast(new PlayerHandler());
					}
				});
			//System.out.println("-Server started!");
			Rukkit.getCurrentPluginManager().serverDone(this);
			log.i("Done! (" + (System.currentTimeMillis()-time) + "ms)");
			ChannelFuture cf = sbs.bind(port).sync();
			cf.channel().closeFuture().sync();
		}finally{
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
}
