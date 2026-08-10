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
import cn.rukkit.network.ConnectionState;
import cn.rukkit.network.command.GameCommand;
import cn.rukkit.network.core.packet.Packet;
import cn.rukkit.network.core.packet.UniversalPacket;
import cn.rukkit.network.NetworkTick;
import cn.rukkit.util.Vote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
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
    private static final int SYNC_TIMEOUT_MILLIS = 5000;
    private static final int SYNC_POLL_INTERVAL_MILLIS = 50;

    public PlayerManager playerManager;
    public RoomConnectionManager connectionManager;
    private final RoomCommandQueue commandQuere = new RoomCommandQueue();
    private final Object commandDispatchLock = new Object();

    public RoundConfig config;
    /** Approximate network window in milliseconds; scheduling uses nanoseconds. */
    public int stepRate = NetworkTick.WINDOW_PERIOD_MILLIS;
    public volatile int currentStep = 0;
    public int checkSumFrame = 0;
    public final AtomicInteger checkSumReceived = new AtomicInteger();
    public int syncCount = 0;
    public int roomId;

    private volatile boolean checkRequested = false;
    public SaveData lastNoStopSave;
    private volatile boolean gameStarted = false;
    private volatile boolean isPaused = false;
    private ScheduledFuture<?> gameTaskFuture;
    private ScheduledFuture<?> syncTaskFuture;
    private long syncDeadline;
    private ScheduledFuture<?> checkSumTaskFuture;
    private long checkSumDeadline;
    private SaveManager saveManager;

    public Vote vote;

    @Override
    public String toString() {
        return MessageFormat.format(
                "NetworkRoom [id = {0}, isGaming = {1}, isPaused = {2}, currentStep = {3}, stepRate = {4}]",
                roomId, isGaming(), isPaused, currentStep, stepRate);
    }

    public ServerRoom(int id) {
        this(id, Rukkit.getRoundConfig());
    }

    public ServerRoom(int id, RoundConfig defaultConfig) {
        roomId = id;
        playerManager = new PlayerManager(this, Rukkit.getConfig().maxPlayer);
        connectionManager = new RoomConnectionManager(this);
        saveManager = new SaveManager(this);
        config = new RoundConfig(defaultConfig);
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
            synchronized (ServerRoom.this) {
                if (!checkRequested) {
                    finishCheckSumTask();
                    return;
                }

                if (connectionManager == null) {
                    checkRequested = false;
                    finishCheckSumTask();
                    return;
                }

                int connectionCount = connectionManager.size();
                if (connectionCount <= 0
                        || checkSumReceived.get() >= connectionCount
                        || System.currentTimeMillis() >= checkSumDeadline) {
                    if (checkSumReceived.get() < connectionCount) {
                        LoggerFactory.getLogger("CheckSum Task Room #" + roomId)
                                .warn("Checksum response timeout: {}/{}",
                                        checkSumReceived.get(), connectionCount);
                    }
                    check(0);
                    checkRequested = false;
                    finishCheckSumTask();
                }
            }
        }
    }

    private void finishCheckSumTask() {
        ScheduledFuture<?> task = checkSumTaskFuture;
        checkSumTaskFuture = null;
        if (task != null) {
            task.cancel(false);
        }
    }

    public class GameTask implements Runnable {
        @Override
        public void run() {
            RukkitConfig cfg = Rukkit.getConfig();
            if (!isPaused) {
                currentStep += NetworkTick.FRAMES_PER_WINDOW;
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

            dispatchQueuedCommands();
        }
    }

    public class NonStopGameTask implements Runnable {
        @Override
        public void run() {
            RukkitConfig cfg = Rukkit.getConfig();
            if (!isPaused) {
                currentStep += NetworkTick.FRAMES_PER_WINDOW;
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

            dispatchQueuedCommands();
        }
    }

    public class SyncTask implements Runnable {
        @Override
        public void run() {
            Logger syncLog = LoggerFactory.getLogger("SyncTask #" + roomId);
            synchronized (ServerRoom.this) {
                if (syncTaskFuture == null || syncTaskFuture.isCancelled()) {
                    return;
                }

                if (connectionManager == null) {
                    finishSyncTask();
                    return;
                }

                SaveData save = connectionManager.getAvailableSave();
                if (save != null) {
                    try {
                        saveManager.setLastSave(save);
                        saveManager.sendLastSaveToAll(false);
                        syncCount++;
                        setPaused(false);
                        finishSyncTask();
                    } catch (IOException e) {
                        syncLog.warn("A exception occurred.", e);
                        finishSyncTask();
                        stopGame();
                    }
                } else if (System.currentTimeMillis() >= syncDeadline) {
                    syncLog.warn("Sync failed!");
                    setPaused(false);
                    finishSyncTask();
                }
            }
        }
    }

    private void finishSyncTask() {
        ScheduledFuture<?> task = syncTaskFuture;
        syncTaskFuture = null;
        if (task != null) {
            task.cancel(false);
        }
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        isPaused = paused;
    }

    /**
     * Sends one room tick containing all commands currently pending. The
     * dispatch lock is separate from the queue lock so producers can enqueue
     * while a packet is being encoded, while sync and normal ticks cannot
     * overtake each other.
     */
    private void dispatchQueuedCommands() {
        synchronized (commandDispatchLock) {
            if (isPaused) {
                return;
            }

            List<GameCommand> commands = commandQuere.drain();
            try {
                if (commands.isEmpty()) {
                    connectionManager.broadcast(UniversalPacket.emptyCommand(currentStep));
                } else {
                    connectionManager.broadcast(
                            UniversalPacket.gameCommands(currentStep, commands));
                }
            } catch (IOException e) {
                // Do not silently lose commands if packet construction fails.
                commandQuere.prepend(commands);
                log.warn("Failed to build command tick for room {}", roomId, e);
            }
        }
    }

    /**
     * Completes the command boundary before a resync save is requested. The
     * room is already paused when this method is called, but commands that
     * arrived before the pause must be sent before the save request.
     */
    private void flushQueuedCommandsForSync() throws IOException {
        synchronized (commandDispatchLock) {
            List<GameCommand> commands = commandQuere.drain();
            if (commands.isEmpty()) {
                return;
            }
            try {
                connectionManager.broadcast(
                        UniversalPacket.gameCommands(currentStep, commands));
            } catch (IOException e) {
                commandQuere.prepend(commands);
                throw e;
            }
        }
    }

    public void stopGame() {
        stopGame(false);
    }

    public void doChecksum() {
        synchronized (this) {
            if (connectionManager == null || checkRequested) {
                return;
            }
            checkRequested = true;
            checkSumReceived.set(0);
            checkSumDeadline = System.currentTimeMillis() + SYNC_TIMEOUT_MILLIS;
            for (ServerRoomConnection connection : connectionManager.connections) {
                connection.doChecksum();
            }
            checkSumTaskFuture = Rukkit.getThreadManager().schedule(
                    new CheckSumTask(), SYNC_POLL_INTERVAL_MILLIS, SYNC_POLL_INTERVAL_MILLIS);
        }
    }

    public synchronized void stopGame(boolean returnToBattleroom) {
        setPaused(true);
        synchronized (commandDispatchLock) {
            commandQuere.clear();
        }
        gameStarted = false;
        currentStep = 0;
        checkSumFrame = 0;
        syncCount = 0;
        checkRequested = false;
        for (NetworkPlayer player : playerManager.getPlayerArray()) {
            if (player != null && !player.isEmpty) {
                player.endGameActivityTracking();
            }
        }
        if (checkSumTaskFuture != null) {
            checkSumTaskFuture.cancel(true);
            checkSumTaskFuture = null;
        }
        if (syncTaskFuture != null) {
            syncTaskFuture.cancel(true);
            syncTaskFuture = null;
        }
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
        RoomStopGameEvent.getListenerList().callListeners(new RoomStopGameEvent(this));
    }

    public void broadcast(Packet packet) {
        connectionManager.broadcast(packet);
    }

    public void discard() {
        setPaused(true);
        synchronized (commandDispatchLock) {
            commandQuere.clear();
        }
        gameStarted = false;
        checkRequested = false;
        if (checkSumTaskFuture != null) {
            checkSumTaskFuture.cancel(true);
            checkSumTaskFuture = null;
        }
        if (syncTaskFuture != null) {
            syncTaskFuture.cancel(true);
            syncTaskFuture = null;
        }
        playerManager.reset();
        connectionManager.disconnect();
        connectionManager.clearAllSaveData();
        playerManager = null;
        connectionManager = null;
    }

    public boolean isGaming() {
        if (gameStarted) {
            return true;
        }
        // Keep compatibility with callers that restore a running room by
        // restoring its tick, while avoiding a start-up window where the
        // first tick has not been emitted yet.
        if (currentStep > 0) {
            gameStarted = true;
            return true;
        }
        return false;
    }

    public synchronized void syncGame() {
        if (connectionManager == null
                || (syncTaskFuture != null && !syncTaskFuture.isDone())) {
            return;
        }
        connectionManager.clearAllSaveData();
        setPaused(true);
        try {
            flushQueuedCommandsForSync();
            connectionManager.broadcast(UniversalPacket.sendPullSave(ServerRoom.this));
        } catch (IOException e) {
            setPaused(false);
            stopGame();
            return;
        }
        syncDeadline = System.currentTimeMillis() + SYNC_TIMEOUT_MILLIS;
        syncTaskFuture = Rukkit.getThreadManager().schedule(
                new SyncTask(), SYNC_POLL_INTERVAL_MILLIS, SYNC_POLL_INTERVAL_MILLIS);
    }

    public synchronized void startGame() {
        if (gameStarted || currentStep > 0) {
            return;
        }
        try {
            synchronized (commandDispatchLock) {
                commandQuere.clear();
            }
            setPaused(false);
            Packet gameStartPacket = UniversalPacket.gameStart(config);
            gameStarted = true;
            connectionManager.broadcast(gameStartPacket);
            for (NetworkPlayer player : playerManager.getPlayerArray()) {
                if (player != null && !player.isEmpty) {
                    player.beginGameActivityTracking();
                }
            }
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
                connection.handler.setState(ConnectionState.IN_GAME);
            }
            gameTaskFuture = Rukkit.getThreadManager().scheduleAtFixedRate(
                    new GameTask(),
                    NetworkTick.WINDOW_PERIOD_NANOS,
                    NetworkTick.WINDOW_PERIOD_NANOS,
                    java.util.concurrent.TimeUnit.NANOSECONDS);
            RoomStartGameEvent.getListenerList().callListeners(new RoomStartGameEvent(this));
        } catch (IOException ignored) {
            gameStarted = false;
        }
    }

    public void changeMapWhileRunning(String mapName, int type) {
        synchronized (commandDispatchLock) {
            commandQuere.clear();
        }
        config.mapName = mapName;
        config.mapType = type;
        try {
            connectionManager.broadcast(UniversalPacket.gameStart(config));
            if (config.sharedControl) {
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
            synchronized (commandDispatchLock) {
                if (isPaused()) {
                    return;
                }
                commandQuere.addLast(command);
            }
        } else {
            if (isPaused()) {
                return;
            }
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
