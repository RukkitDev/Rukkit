package cn.rukkit.network;

import cn.rukkit.Rukkit;
import cn.rukkit.config.RoundConfig;
import cn.rukkit.config.RukkitConfig;
import cn.rukkit.event.room.RoomStartGameEvent;
import cn.rukkit.event.room.RoomStopGameEvent;
import cn.rukkit.game.NetworkPlayer;
import cn.rukkit.game.PlayerManager;
import cn.rukkit.game.SaveData;
import cn.rukkit.game.SaveManager;
import cn.rukkit.network.command.GameCommand;
import cn.rukkit.network.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.ScheduledFuture;

public class NetworkRoom {
    public PlayerManager playerManager;
    public RoomConnectionManager connectionManager;
    /**
     * 命令列表。在采用更稳定的同步(useCommandQuere)时会启用，减少同步错误但是会提高操作延迟。
     */
    private LinkedList<GameCommand> commandQuere = new LinkedList<GameCommand>();

    public RoundConfig config;
    public int stepRate = 200;
    public int currentStep = 0;
    public int roomId;

    /**
     * NoStop模式下的房间存档
     */
    public SaveData lastNoStopSave;
    private boolean isGaming = false;
    private boolean isPaused = false;
    private ScheduledFuture gameTaskFuture;
    private SaveManager saveManager;

    public NetworkRoom(int id) {
        // 指定房间id
        roomId = id;
        //初始化玩家控制器，连接控制器，和存档管理器
        playerManager = new PlayerManager(this, Rukkit.getConfig().maxPlayer);
        connectionManager = new RoomConnectionManager(this);
        saveManager = new SaveManager(this);
        config = Rukkit.getRoundConfig();
    }

    public class GameTask implements Runnable {
        @Override
        public void run() {
            RukkitConfig cfg = Rukkit.getConfig();
            if (!isPaused) {
                // Add step
                currentStep += 10;
            }
            if (connectionManager.size() <= 0) {
                stopGame();
                Rukkit.getThreadManager().shutdownTask(gameTaskFuture);
                return;
            }

            if (connectionManager.size() <= 1 && !cfg.singlePlayerMode) {
                connectionManager.broadcastServerMessage("1 player left.Auto disconnecting...");
                stopGame();
                Rukkit.getThreadManager().shutdownTask(gameTaskFuture);
                return;
            }

            synchronized (commandQuere) {
                //log.debug("tick:" + tickTime);
                try {
                    if (commandQuere.isEmpty() && !isPaused) {
                        connectionManager.broadcast(Packet.emptyCommand(currentStep));
                    } else {
                        while(!commandQuere.isEmpty() && !isPaused){
                            GameCommand cmd = commandQuere.removeLast();
                            connectionManager.broadcast(new Packet().gameCommand(currentStep, cmd));
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
            if (!isPaused) {
                // Add tickTime
                currentStep += 10;
            }

            // If playercount == 1 then have a sync and pauseGame;
            if (connectionManager.size() == 1 && !cfg.singlePlayerMode && !isPaused) {
                connectionManager.broadcastServerMessage("1 player left.We will have a sync and pause game...");
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
            if (connectionManager.size() <= 0) {
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
                        connectionManager.broadcast(Packet.emptyCommand(currentStep));
                    } else {
                        while(!commandQuere.isEmpty() && !isPaused){
                            GameCommand cmd = commandQuere.removeLast();
                            connectionManager.broadcast(new Packet().gameCommand(currentStep, cmd));
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
            Logger log = LoggerFactory.getLogger("SyncTask #" + roomId);
            connectionManager.clearAllSaveData();
            setPaused(true);
            //自定义地图的同步逻辑
            try {
                //Rukkit.getSaveManager().sendDefaultSaveToAll();
                //Rukkit.getConnectionManager().broadcast(Packet.syncCheckSum());
                connectionManager.broadcast(Packet.sendPullSave(NetworkRoom.this));
                SaveData save;
                long time = System.currentTimeMillis();
                while (true) {
                    save = connectionManager.getAvailableSave();
                    if (save != null) {
                        saveManager.setLastSave(save);
                        saveManager.sendLastSaveToAll(false);
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

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        isPaused = paused;
    }

    /**
     * Stop a round game.
     */
    public void stopGame() {
        // Reset player slot
        playerManager.reset();
        // Reset ticktime
        currentStep = 0;
        // End all connections
        connectionManager.disconnect();
        gameTaskFuture.cancel(true);
        isGaming = false;
        RoomStopGameEvent.getListenerList().callListeners(new RoomStopGameEvent(this));
        //Rukkit.getThreadManager().shutdown();
    }

    /**
     * Broadcast a packet.
     * @param packet the packet wants to broadcast in this room.
     */
    public void broadcast(Packet packet) {
        connectionManager.broadcast(packet);
    }

    public void discard() {
        playerManager.reset();
        connectionManager.disconnect();
        connectionManager.clearAllSaveData();
        playerManager = null;
        connectionManager = null;
    }

    public boolean isGaming() {
//        if (Rukkit.getConfig().nonStopMode) {
//            return true;
//        }
        if (currentStep <= 0) {
            isGaming = false;
        } else {
            isGaming = true;
        }
        return isGaming;
    }

    public void syncGame() {
        Rukkit.getThreadManager().submit(new SyncTask());
    }

    /**
     * starts a round game.
     */
    public void startGame() {
        try {
            connectionManager.broadcast(Packet.gameStart());
            // Set shared control.
            if (Rukkit.getRoundConfig().sharedControl) {
                for (NetworkPlayer p:playerManager.getPlayerArray()) {
                    try {
                        p.isNull();
                        p.isSharingControl = true;
                    } catch (NullPointerException ignored) {continue;}
                }
            }
            // Reset tick time
            currentStep = 0;
            // Broadcast start packet.
            connectionManager.broadcast(Packet.serverInfo(config));
            for(RoomConnection conn : connectionManager.getConnections()) {
                conn.updateTeamList();
            }
            gameTaskFuture = Rukkit.getThreadManager().schedule(new GameTask(), stepRate, stepRate);
            //connectionManager.broadcast()
            isGaming = true;
            RoomStartGameEvent.getListenerList().callListeners(new RoomStartGameEvent(this));
        } catch (IOException ignored) {}
    }

    /**
     * Start a no-stop game.
     * only works if nonStop config == true
     */
//    public void startNoStopGame() {
//        if (Rukkit.getConfig().nonStopMode) {
//            gameTaskFuture = Rukkit.getThreadManager().schedule(new NonStopGameTask(), stepRate, stepRate);
//        }
//    }

    public void changeMapWhileRunning(String mapName, int type) {
        Rukkit.getRoundConfig().mapName = mapName;
        Rukkit.getRoundConfig().mapType = type;
        try {
            connectionManager.broadcast(Packet.gameStart());
            // Set shared control.
            if (Rukkit.getRoundConfig().sharedControl) {
                for (NetworkPlayer p:playerManager.getPlayerArray()) {
                    try {
                        p.isNull();
                        p.isSharingControl = true;
                    } catch (NullPointerException ignored) {continue;}
                }
            }
            // Reset tick time
            currentStep = 0;
            // Broadcast start packet.
            connectionManager.broadcast(Packet.serverInfo(config));
            for(RoomConnection conn : connectionManager.getConnections()) {
                conn.updateTeamList(false);
            }
        } catch (IOException ignored) {}
    }

    public void notifyGameTask() {
        //hreadLock.notify();
        setPaused(false);
    }

    public int getTickTime() {
        return currentStep;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public void addCommand(GameCommand cmd) {
        if (Rukkit.getConfig().useCommandQuere) {
            commandQuere.addLast(cmd);
        } else {
            try {
                broadcast(Packet.gameCommand(this.currentStep, cmd));
            } catch (IOException ignored) {}
        }
    }


}
