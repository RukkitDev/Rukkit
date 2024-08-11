package cn.rukkit.network;

import cn.rukkit.Rukkit;
import cn.rukkit.config.RoundConfig;
import cn.rukkit.config.RukkitConfig;
import cn.rukkit.event.room.RoomStartGameEvent;
import cn.rukkit.event.room.RoomStopGameEvent;
import cn.rukkit.game.*;
import cn.rukkit.network.command.GameCommand;
import cn.rukkit.network.packet.Packet;
import cn.rukkit.util.Vote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

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
    public int checkSumFrame = 0;
    public final AtomicInteger checkSumReceived = new AtomicInteger();
    public int syncCount = 0;
    public int roomId;

    private volatile boolean checkRequested = false;

    /**
     * NoStop模式下的房间存档
     */
    public SaveData lastNoStopSave;
    private boolean isGaming = false;
    private boolean isPaused = false;
    private ScheduledFuture gameTaskFuture;
    private SaveManager saveManager;

    public Vote vote;

    @Override
    public String toString() {
        return MessageFormat.format("NetworkRoom [id = {0}, isGaming = {1}, isPaused = {2}, currentStep = {3}, stepRate = {4}]",
                roomId, isGaming, isPaused, currentStep, stepRate);
    }

    public NetworkRoom(int id) {
        // 指定房间id
        roomId = id;
        //初始化玩家控制器，连接控制器，和存档管理器
        playerManager = new PlayerManager(this, Rukkit.getConfig().maxPlayer);
        connectionManager = new RoomConnectionManager(this);
        saveManager = new SaveManager(this);
        config = Rukkit.getRoundConfig();
        vote = new Vote(this);
    }

    public class CheckSumTask implements Runnable {
        Logger log = LoggerFactory.getLogger("CheckSum Task Room #" + roomId);
        public void check(int recheck) {
            // recheck 失败，同步游戏
            if (recheck >= 3) {
                log.error("Checksum failed!May be a resync needed!");
                syncGame();
                return;
            }
            // 随机选取玩家的checklist进行对比
            CheckSumList list = null;
            int diffcount = 0;
            AtomicInteger time = new AtomicInteger();
            HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
            for (RoomConnection r: connectionManager.getConnections()) {
                if (r.checkSumSent) {
                    map.put(r.lastSyncTick,
                            map.getOrDefault(r.lastSyncTick, 0) + 1);
                }
            }
            AtomicInteger max = new AtomicInteger();
            map.forEach((k, v) -> {
                if (v > max.get()) {
                    max.set(v);
                    time.set(k);
                }
            });
            // log.info("Max sync time: {}", time);
            for (RoomConnection r: connectionManager.getConnections()) {
                // 未发送不检查
                if (!r.checkSumSent) continue;
                // 随机挑选一位玩家的checkList做样本进行检查
                if (list == null) {
                    int rnd = new Random().nextInt(connectionManager.size());
                    if (connectionManager.getConnections().get(rnd).checkSumSent)
                        list = connectionManager.getConnections().get(rnd).player.checkList;
                    else continue;
                }
                // 同步时间检查，若不符合正确的 tick 则不检查
                if (time.get() != r.lastSyncTick) continue;
                // 检查所有数据, 出现问题则diffcount++;
                if (!list.checkData(r.player.checkList)) {
                    diffcount++;
                }
            }
            if (diffcount >= Math.ceil(connectionManager.size() / 2.0) && connectionManager.size() >= 2) {
                log.warn("diffcount {} > {} players!Do recheck!", diffcount, Math.ceil(connectionManager.size() / 2.0));
                check(recheck+1);
                // 如果有 2 人以上不同步则执行游戏同步
            } else if (diffcount >= 2){
                log.info("Desync found.Resyncing game...");
                syncGame();
            } else {
                log.info("Checksum complete!");
            }
            for (RoomConnection r: connectionManager.getConnections()) {
                r.checkSumSent = false;
            }
            log.info("diffcount: {}, maxSyncTime: {}", diffcount, time);
        }
        @Override
        public void run() {
            if (checkRequested) {
                synchronized (checkSumReceived) {
                    while (true) {
                        try {
                            checkSumReceived.wait();
                            if (checkSumReceived.get() >= connectionManager.size()) break;
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                check(0);
                checkRequested = false;
            }
        }
    }

    public class GameTask implements Runnable {
        @Override
        public void run() {
            RukkitConfig cfg = Rukkit.getConfig();
            if (!isPaused) {
                // Add step
                currentStep += 10;
                if (Rukkit.getConfig().checksumSync) {
                    if (currentStep % 300 == 0) {
                        if (!checkRequested) {
                            checkSumReceived.set(0);
                            doChecksum();
                        } else {
                            // 立刻开始检查已经发送的人员而不是重新进行
                            checkSumReceived.set(connectionManager.size());
                            synchronized (checkSumReceived) {
                                checkSumReceived.notify();
                            }
                        }
                    }
                }
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
                            connectionManager.broadcast(Packet.gameCommand(currentStep, cmd));
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
                        syncCount++;
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

    public void stopGame() {
        stopGame(false);
    }

    public void doChecksum() {
        checkRequested = true;
        for (RoomConnection r: connectionManager.connections) {
            r.doChecksum();
        }
        Rukkit.getThreadManager().submit(new CheckSumTask());
    }

    /**
     * Stop a round game.
     */
    public void stopGame(boolean isRuturn) {

        // Reset ticktime and checksum
        currentStep = 0;
        checkSumFrame = 0;
        syncCount = 0;
        // End all connections
        if (isRuturn) {
            try {
                playerManager.clearDisconnectedPlayers();
                connectionManager.broadcast(Packet.packetReturnToBattleroom());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            // Reset player slot
            playerManager.reset();
            connectionManager.disconnect();
        }
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
                        p.isSharingControl = false;
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
                        // p.isSharingControl = true;
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

    public void summonUnit(String unitName, float x, float y, int player) {
        try {
            broadcast(Packet.gameSummon(getCurrentStep(), unitName, x, y, player));
        } catch (IOException e) {

        }
    }

    public void summonUnit(String unitName, float x, float y) {
        try {
            broadcast(Packet.gameSummon(getCurrentStep(), unitName, x, y));
        } catch (IOException e) {

        }
    }
}
