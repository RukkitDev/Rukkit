/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

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
	private ChannelFuture serverFuture;
	public SaveData lastNoStopSave;

	// For save server performance.
	private Object threadLock = new Object();

	//private boolean isGaming = false;
	private LinkedList<GameCommand> commandQuere = new LinkedList<GameCommand>();
	private ScheduledFuture gameTaskFuture;
	private NioEventLoopGroup bossGroup, workerGroup;

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

	public class NonStopGameTask implements Runnable {
		@Override
		public void run() {
			RukkitConfig cfg = Rukkit.getConfig();
			ConnectionManager connMgr = Rukkit.getConnectionManager();
			if (!isPaused) {
				// Add tickTime
				tickTime += 10;
			}

			// If playercount == 1 then have a sync and pauseGame;
			if (connMgr.size() == 1 && !cfg.singlePlayerMode && !isPaused) {
				connMgr.broadcastServerMessage("1 player left.We will have a sync and pause game...");
				syncGame();
				/*synchronized (threadLock) {
					try {
						threadLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}*/
				setPaused(true);
				return;
			}

			/* If playerCount == 0 then pauseGame
			*/
			if (connMgr.size() <= 0) {
				/*synchronized (threadLock) {
					try {
						threadLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}*/
				setPaused(true);
				return;
			}

			// If using query mode:
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

	/**
	 * Game Sync task.
	 */
	public class SyncTask implements Runnable {
        @Override
        public void run() {
			Rukkit.getConnectionManager().clearAllSaveData();
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
						//If no-stop enabled use lastNoStopSave.
						if (Rukkit.getConfig().nonStopMode) {
							lastNoStopSave = save;
						}
                        Rukkit.getSaveManager().setLastSave(save);
                        Rukkit.getSaveManager().sendLastSaveToAll(false);
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

	public void notifyGameTask() {
		//hreadLock.notify();
		setPaused(false);
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
	 * Start a no-stop game.
	 * only works if nonStop config == true
	 */
	public void startNoStopGame() {
		if (Rukkit.getConfig().nonStopMode) {
			gameTaskFuture = Rukkit.getThreadManager().schedule(new NonStopGameTask(), 200, 200);
		}
	}
	
	public void changeMapWhileRunning(String mapName, int type) {
		Rukkit.getRoundConfig().mapName = mapName;
		Rukkit.getRoundConfig().mapType = type;
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
				conn.updateTeamList(false);
			}
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

	public void stopServer() {
		bossGroup.shutdownGracefully();
		workerGroup.shutdownGracefully();
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
						log.info("Done! (" + (System.currentTimeMillis() - time) + "ms)");
						Rukkit.setStarted(true);
						if (Rukkit.getConfig().nonStopMode) {
							log.info("Server is running on non-stop mode.Game auto starting...");
							startNoStopGame();
							tickTime = 10;
						}
						// TODO: Implement this method
					}
				}).start();
			serverFuture = sbs.bind(port).sync();
			serverFuture.channel().closeFuture().sync();
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
