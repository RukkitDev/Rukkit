/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.room;

import cn.rukkit.Rukkit;
import cn.rukkit.config.RoundConfig;
import cn.rukkit.config.RukkitConfig;
import cn.rukkit.event.room.RoomStartGameEvent;
import cn.rukkit.event.room.RoomStopGameEvent;
import cn.rukkit.game.CheckSumList;
import cn.rukkit.game.NetworkPlayer;
import cn.rukkit.game.PlayerManager;
import cn.rukkit.game.SaveData;
import cn.rukkit.game.SaveManager;
import cn.rukkit.network.command.GameCommand;
import cn.rukkit.network.core.packet.Packet;
import cn.rukkit.network.core.packet.UniversalPacket;
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

/**
 * The master branch's NetworkRoom state machine moved to the new packet layer.
 *
 * <p>This class deliberately keeps the master behavior and GameCommand model;
 * unrelated network and simulation features are outside this migration.</p>
 */
public class ServerRoom {
    private static final Logger log = LoggerFactory.getLogger(ServerRoom.class);

    public PlayerManager playerManager;
    public RoomConnectionManager connectionManager;
    private LinkedList<GameCommand> commandQuere = new LinkedList<GameCommand>();

    public RoundConfig config;
    public int stepRate = 200;
    public int currentStep = 0;
    public int checkSumFrame = 0;
    public final AtomicInteger checkSumReceived = new AtomicInteger();
    public int syncCount = 0;
    public int roomId;

    private volatile boolean checkRequested = false;
    public SaveData lastNoStopSave;
    private boolean isGaming = false;
    private boolean isPaused = false;
    private ScheduledFuture<?> gameTaskFuture;
    private SaveManager saveManager;

    public Vote vote;

    @Override
    public String toString() {
        return MessageFormat.format(
                "NetworkRoom [id = {0}, isGaming = {1}, isPaused = {2}, currentStep = {3}, stepRate = {4}]",
                roomId, isGaming, isPaused, currentStep, stepRate);
    }

    public ServerRoom(int id) {
        roomId = id;
        playerManager = new PlayerManager(this, Rukkit.getConfig().maxPlayer);
        connectionManager = new RoomConnectionManager(this);
        saveManager = new SaveManager(this);
        config = Rukkit.getRoundConfig();
        vote = new Vote(this);
    }

    public class CheckSumTask implements Runnable {
        Logger taskLog = LoggerFactory.getLogger("CheckSum Task Room #" + roomId);

        public void check(int recheck) {
            if (recheck >= 3) {
                taskLog.error("Checksum failed!May be a resync is needed!");
                syncGame();
                return;
            }

            CheckSumList list = null;
            int diffcount = 0;
            AtomicInteger time = new AtomicInteger();
            HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
            for (ServerRoomConnection connection : connectionManager.getConnections()) {
                if (connection.checkSumSent) {
                    map.put(connection.lastSyncTick,
                            map.getOrDefault(connection.lastSyncTick, 0) + 1);
                }
            }
            AtomicInteger max = new AtomicInteger();
            map.forEach((tick, count) -> {
                if (count > max.get()) {
                    max.set(count);
                    time.set(tick);
                }
            });

            for (ServerRoomConnection connection : connectionManager.getConnections()) {
                if (!connection.checkSumSent) {
                    continue;
                }
                if (list == null) {
                    int random = new Random().nextInt(connectionManager.size());
                    if (connectionManager.getConnections().get(random).checkSumSent) {
                        list = connectionManager.getConnections().get(random).player.checkList;
                    } else {
                        continue;
                    }
                }
                if (time.get() != connection.lastSyncTick) {
                    continue;
                }
                if (!list.checkData(connection.player.checkList)) {
                    diffcount++;
                }
            }

            if (diffcount >= Math.ceil(connectionManager.size() / 2.0)
                    && connectionManager.size() >= 2) {
                taskLog.warn("diffcount {} > {} players!Do recheck!", diffcount,
                        Math.ceil(connectionManager.size() / 2.0));
                check(recheck + 1);
            } else if (diffcount >= 2) {
                taskLog.info("Desync found.Resyncing game...");
                syncGame();
            } else {
                taskLog.info("Checksum complete!");
            }

            for (ServerRoomConnection connection : connectionManager.getConnections()) {
                connection.checkSumSent = false;
            }
            taskLog.info("diffcount: {}, maxSyncTime: {}", diffcount, time);
        }

        @Override
        public void run() {
            if (checkRequested) {
                synchronized (checkSumReceived) {
                    while (true) {
                        try {
                            checkSumReceived.wait();
                            if (checkSumReceived.get() >= connectionManager.size()) {
                                break;
                            }
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
                currentStep += 10;
                if (cfg.checksumSync && currentStep % 300 == 0) {
                    if (!checkRequested) {
                        checkSumReceived.set(0);
                        doChecksum();
                    } else {
                        checkSumReceived.set(connectionManager.size());
                        synchronized (checkSumReceived) {
                            checkSumReceived.notifyAll();
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
                try {
                    if (commandQuere.isEmpty() && !isPaused) {
                        connectionManager.broadcast(UniversalPacket.emptyCommand(currentStep));
                    } else {
                        while (!commandQuere.isEmpty() && !isPaused) {
                            GameCommand command = commandQuere.removeLast();
                            connectionManager.broadcast(
                                    UniversalPacket.gameCommand(currentStep, command));
                        }
                    }
                } catch (IOException ignored) {
                }
            }
        }
    }

    public class NonStopGameTask implements Runnable {
        @Override
        public void run() {
            RukkitConfig cfg = Rukkit.getConfig();
            if (!isPaused) {
                currentStep += 10;
            }
            if (connectionManager.size() == 1 && !cfg.singlePlayerMode && !isPaused) {
                connectionManager.broadcastServerMessage(
                        "1 player left.We will have a sync and pause game...");
                syncGame();
                setPaused(true);
                return;
            }
            if (connectionManager.size() <= 0) {
                setPaused(true);
                return;
            }

            synchronized (commandQuere) {
                try {
                    if (commandQuere.isEmpty() && !isPaused) {
                        connectionManager.broadcast(UniversalPacket.emptyCommand(currentStep));
                    } else {
                        while (!commandQuere.isEmpty() && !isPaused) {
                            GameCommand command = commandQuere.removeLast();
                            connectionManager.broadcast(
                                    UniversalPacket.gameCommand(currentStep, command));
                        }
                    }
                } catch (IOException ignored) {
                }
            }
        }
    }

    public class SyncTask implements Runnable {
        @Override
        public void run() {
            Logger syncLog = LoggerFactory.getLogger("SyncTask #" + roomId);
            connectionManager.clearAllSaveData();
            setPaused(true);
            try {
                connectionManager.broadcast(UniversalPacket.sendPullSave(ServerRoom.this));
                SaveData save;
                long time = System.currentTimeMillis();
                while (true) {
                    save = connectionManager.getAvailableSave();
                    if (save != null) {
                        saveManager.setLastSave(save);
                        saveManager.sendLastSaveToAll(false);
                        syncCount++;
                        setPaused(false);
                        break;
                    } else if (System.currentTimeMillis() - time > 5000) {
                        syncLog.warn("Sync failed!");
                        setPaused(false);
                        break;
                    }
                }
            } catch (IOException e) {
                syncLog.warn("A exception occurred.", e);
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
        for (ServerRoomConnection connection : connectionManager.connections) {
            connection.doChecksum();
        }
        Rukkit.getThreadManager().submit(new CheckSumTask());
    }

    public void stopGame(boolean returnToBattleroom) {
        currentStep = 0;
        checkSumFrame = 0;
        syncCount = 0;
        if (returnToBattleroom) {
            try {
                playerManager.clearDisconnectedPlayers();
                connectionManager.broadcast(UniversalPacket.packetReturnToBattleroom());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            playerManager.reset();
            connectionManager.disconnect();
        }
        if (gameTaskFuture != null) {
            gameTaskFuture.cancel(true);
        }
        isGaming = false;
        RoomStopGameEvent.getListenerList().callListeners(new RoomStopGameEvent(this));
    }

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

    public void startGame() {
        try {
            connectionManager.broadcast(UniversalPacket.gameStart());
            if (config.sharedControl) {
                for (NetworkPlayer player : playerManager.getPlayerArray()) {
                    try {
                        player.isNull();
                        player.isSharingControl = false;
                    } catch (NullPointerException ignored) {
                        continue;
                    }
                }
            }
            currentStep = 0;
            connectionManager.broadcast(UniversalPacket.serverInfo(config));
            for (ServerRoomConnection connection : connectionManager.getConnections()) {
                connection.updateTeamList();
            }
            gameTaskFuture = Rukkit.getThreadManager().schedule(new GameTask(), stepRate, stepRate);
            isGaming = true;
            RoomStartGameEvent.getListenerList().callListeners(new RoomStartGameEvent(this));
        } catch (IOException ignored) {
        }
    }

    public void changeMapWhileRunning(String mapName, int type) {
        Rukkit.getRoundConfig().mapName = mapName;
        Rukkit.getRoundConfig().mapType = type;
        try {
            connectionManager.broadcast(UniversalPacket.gameStart());
            if (Rukkit.getRoundConfig().sharedControl) {
                for (NetworkPlayer player : playerManager.getPlayerArray()) {
                    try {
                        player.isNull();
                    } catch (NullPointerException ignored) {
                        continue;
                    }
                }
            }
            currentStep = 0;
            connectionManager.broadcast(UniversalPacket.serverInfo(config));
            for (ServerRoomConnection connection : connectionManager.getConnections()) {
                connection.updateTeamList(false);
            }
        } catch (IOException ignored) {
        }
    }

    public void notifyGameTask() {
        setPaused(false);
    }

    public int getTickTime() {
        return currentStep;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public void addCommand(GameCommand command) {
        if (Rukkit.getConfig().useCommandQuere) {
            commandQuere.addLast(command);
        } else {
            try {
                broadcast(UniversalPacket.gameCommand(currentStep, command));
            } catch (IOException ignored) {
            }
        }
    }

    public void summonUnit(String unitName, float x, float y, int player) {
        try {
            broadcast(UniversalPacket.gameSummon(currentStep, unitName, x, y, player));
        } catch (IOException ignored) {
        }
    }

    public void summonUnit(String unitName, float x, float y) {
        try {
            broadcast(UniversalPacket.gameSummon(currentStep, unitName, x, y));
        } catch (IOException ignored) {
        }
    }
}
