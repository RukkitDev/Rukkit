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
import io.rukkit.event.game.*;


public final class Game
{
	
	private int port;
	
	private static Logger log = new Logger("GameServer");
	volatile private int tickTime = -100;
	private volatile LinkedList<GameCommand> commandQuere = new LinkedList<GameCommand>();
	public static ArrayList<ModUnit> defaultModUnits = new ArrayList<ModUnit>();
	//Set a channelGroup
	
	public synchronized int getTickTime() {
		return tickTime;
	}
	
	public synchronized void addGameCommand(GameCommand cmd) {
		commandQuere.add(cmd);
	}
	
	public class GameTickTask extends TimerTask
	{
		
		@Override
		public void run()
		{
			try{
				tickTime += 10;
			if(ChannelGroups.size() == 1 && !ServerProperties.singlePlayerMode){
				tickTime = -100;
				new Logger("Game").d("Game stopped!");
				ChannelGroups.broadcast(new Packet().chat("Server", "只剩你一人，自动断线！Only one player in server, Reset!", -1));
				GameStopEvent.getListenerList().callListeners(new GameStopEvent());
				PlayerGroup.reset();
				cancel();
			}
			
			if(ChannelGroups.size() <= 0){
				tickTime = -100;
				new Logger("Game").d("Game stopped!");
				GameStopEvent.getListenerList().callListeners(new GameStopEvent());
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
		}
		
	}
	
	private Timer gameTickTask = new Timer();
	
	public Game(int port){
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
			GameStartEvent.getListenerList().callListeners(new GameStartEvent());
			gameTickTask.schedule(new GameTickTask(), 0, 200);
			
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public boolean isGaming(){
		return tickTime >= 0;
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
			/*.handler(new LoggingHandler(LogLevel.ERROR))*/
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
			new Thread(new Runnable() {
					@Override
					public void run()
					{
						Rukkit.getCurrentPluginManager().serverDone(Game.this);
						log.i("Done! (" + (System.currentTimeMillis()-time) + "ms)");
						//RukkitConsoleHandler handler = new RukkitConsoleHandler();
						//handler.start();
						//Rukkit.setConsole(handler);
						// TODO: Implement this method
					}
			}).start();
			ChannelFuture cf = sbs.bind(port).sync();
			cf.channel().closeFuture().sync();
		}catch(Exception e) {
			log.e(e);
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
