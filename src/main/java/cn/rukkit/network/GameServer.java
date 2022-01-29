package cn.rukkit.network;

import cn.rukkit.*;
import cn.rukkit.game.NetworkPlayer;
import cn.rukkit.network.packet.*;
import io.netty.bootstrap.*;
import io.netty.channel.*;
import io.netty.channel.nio.*;
import io.netty.channel.socket.*;
import io.netty.channel.socket.nio.*;
import org.slf4j.*;
import io.netty.handler.logging.*;

import java.io.IOException;
import cn.rukkit.config.*;
import cn.rukkit.game.*;
import java.util.*;
import cn.rukkit.network.command.*;
import java.util.concurrent.*;

public class GameServer {
	private Logger log = LoggerFactory.getLogger(GameServer.class);

	private int port;
    private boolean isPaused;
	private int tickTime = 0;
	//private boolean isGaming = false;
	private LinkedList<GameCommand> commandQuere = new LinkedList<GameCommand>();
	private ScheduledFuture gameTaskFuture;
	public class GameTask implements Runnable {
		@Override
		public void run() {
			RukkitConfig cfg = Rukkit.getConfig();
			ConnectionManager connMgr = Rukkit.getConnectionManager();
            if (!isPaused) {
                // Add ticktime
                tickTime += 10;
            }
			if (connMgr.size() <= 0) {
				stopGame();
				Rukkit.getThreadManager().shutdownTask(gameTaskFuture);
				return;
			}

			if (connMgr.size() <= 1 && !cfg.singlePlayerMode) {
				connMgr.broadcastServerMessage("1 player left.Auto disconnecting...");
				stopGame();
				Rukkit.getThreadManager().shutdownTask(gameTaskFuture);
				return;
			}

			synchronized (commandQuere) {
				//log.debug("tick:" + tickTime);
				try {
					if (commandQuere.isEmpty() && !isPaused) {
						connMgr.broadcast(Packet.emptyCommand(tickTime));
					} else {
						while(!commandQuere.isEmpty() && !isPaused){
							GameCommand cmd = commandQuere.removeLast();
							connMgr.broadcast(new Packet().gameCommand(tickTime, cmd));
						}
					}
				} catch (IOException ignored) {}
			}
		}
	}
    
    public class SyncTask implements Runnable {
        @Override
        public void run() {
            setPaused(true);
			//自定义地图的同步逻辑
			
            try {
                //Rukkit.getSaveManager().sendDefaultSaveToAll();
				//Rukkit.getConnectionManager().broadcast(Packet.syncCheckSum());
                Rukkit.getConnectionManager().broadcast(Packet.sendPullSave());
                SaveData save;
                long time = System.currentTimeMillis();
                while (true) {
                    save = Rukkit.getConnectionManager().getAvailableSave();
                    if (save != null) {
                        Rukkit.getSaveManager().setLastSave(save);
                        Rukkit.getSaveManager().sendLastSaveToAll(false);
                        Rukkit.getConnectionManager().clearAllSaveData();
						//save.loadSave();
                        //tickTime = save.time;
                        setPaused(false);
                        break;
                    } else if (System.currentTimeMillis() - time > 5000) {
                        log.warn("Sync failed!");
                        setPaused(false);
                        break;
                    }
                }

            } catch (IOException e) {
				log.warn("A Exception occured.", e);
				stopGame();
            }
        }
    }

	public GameServer(int port) {
		this.port = port;
	}

	public boolean isGaming() {
		return tickTime > 0;
	}
	
	public void addCommand(GameCommand cmd) {
		if (Rukkit.getConfig().useCommandQuere) {
			commandQuere.addLast(cmd);
		}
	}
	
	public int getTickTime() {
		return tickTime;
	}

	/**
	 * starts a round game.
	 */
	public void startGame() {
		try {
			ConnectionManager connectionManager = Rukkit.getConnectionManager();
			connectionManager.broadcast(Packet.gameStart());
			// Set shared control.
			if (Rukkit.getRoundConfig().sharedControl) {
				for (NetworkPlayer p:Rukkit.getConnectionManager().getPlayerManager().getPlayerArray()) {
					try {
						p.isNull();
						p.isSharingControl = true;
					} catch (NullPointerException ignored) {continue;}
				}
			}
			// Reset tick time
			tickTime = 0;
			// Broadcast start packet.
			connectionManager.broadcast(Packet.serverInfo());
			for(Connection conn : connectionManager.getConnections()) {
				conn.updateTeamList();
			}
			gameTaskFuture = Rukkit.getThreadManager().schedule(new GameTask(), 200, 200);
			//connectionManager.broadcast()
		} catch (IOException ignored) {}
	}

	/**
	 * Stop a round game.
	 */
	public void stopGame() {
		// Reset player slot
		Rukkit.getConnectionManager().getPlayerManager().reset();
		// Reset ticktime
		tickTime = 0;
		// End all connections
		Rukkit.getConnectionManager().disconnect();
		gameTaskFuture.cancel(true);
		//Rukkit.getThreadManager().shutdown();
	}
    
    /**
    * Sync a Game.
    *
    */
    public void syncGame() {
        Rukkit.getThreadManager().submit(new SyncTask());
    }
    
    public void setPaused(boolean paused) {
        isPaused = paused;
    }
    
    public boolean isPaused() {
        return isPaused;
    }
    
	/**
	 * Stops server.
	 */
	public void stop() {

	}


	/**
	 * Start a Server.
	 */
	public void action(final long time) throws InterruptedException {
		// 用来接收进来的连接
		EventLoopGroup bossGroup = new NioEventLoopGroup(); 
		// 用来处理已经被接收的连接，一旦bossGroup接收到连接，就会把连接信息注册到workerGroup上
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap sbs = new ServerBootstrap();
			sbs.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.option(ChannelOption.SO_BACKLOG, 128)
				.childOption(ChannelOption.SO_KEEPALIVE, true)
				//.handler(new LoggingHandler(log))
				.handler(new LoggingHandler(log.getName(), LogLevel.ERROR))
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
						//Rukkit.getCurrentPluginManager().serverDone(Game.this);
						log.info("Done! (" + (System.currentTimeMillis() - time) + "ms)");
						//RukkitConsoleHandler handler = new RukkitConsoleHandler();
						//handler.start();
						//Rukkit.setConsole(handler);
						// TODO: Implement this method
					}
				}).start();
			ChannelFuture cf = sbs.bind(port).sync();
			cf.channel().closeFuture().sync();
		} catch (Exception e) {
			log.error("A error occoured: ", e);
			Rukkit.shutdown(e.getMessage());
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
			return;
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
}
