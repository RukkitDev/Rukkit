/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */
/*
 * Copyright 2025 Micro(MCLDY@outlook.com) and contributors.
 * 
 * 本衍生作品基于 AGPLv3 许可证
 * This derivative work is licensed under AGPLv3
 */

package cn.rukkit.network.room;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.rukkit.Rukkit;
import cn.rukkit.config.RoundConfig;
import cn.rukkit.config.RukkitConfig;
import cn.rukkit.event.room.RoomStartGameEvent;
import cn.rukkit.event.room.RoomStopGameEvent;
import cn.rukkit.game.CheckSumList;
import cn.rukkit.game.CheckSumList.ChecksumItem;
import cn.rukkit.game.NetworkPlayer;
import cn.rukkit.game.PlayerManager;
import cn.rukkit.game.SaveData;
import cn.rukkit.game.SaveManager;
import cn.rukkit.game.VirtualWorld;
import cn.rukkit.game.map.MapParser;
import cn.rukkit.network.command.NewGameCommand;
import cn.rukkit.network.core.packet.Packet;
import cn.rukkit.network.core.packet.UniversalPacket;
import cn.rukkit.util.Vote;

public class ServerRoom {
    private static final Logger log = LoggerFactory.getLogger(ServerRoom.class);
    public int roomId;
    public PlayerManager playerManager;
    /**
     * 命令列表。在采用更稳定的同步(useCommandQuere)时会启用，减少同步错误但是会提高操作延迟。
     */
    private LinkedList<NewGameCommand> commandQuere = new LinkedList<NewGameCommand>();

    public RoomConnectionManager connectionManager;
    public RoundConfig config;

    public VirtualWorld world;// 虚拟世界 可以利用其获取游戏信息完成同步&校验

    public int stepRate = 200;
    public int currentStep = 0;
    public int checkSumFrame = 0;
    public final AtomicInteger checkSumReceived = new AtomicInteger();// 已应答checksum包的连接数量
    private volatile boolean checkRequested = false;// 正在等待c端应答校验包
    public CheckSumList checkList = new CheckSumList();// 可信校验和列表(可能会慢点)
    public int syncCount = 0;

    /**
     * NoStop模式下的房间存档
     */
    public SaveData lastNoStopSave;
    private boolean isGaming = false;
    private boolean isPaused = false;
    private ScheduledFuture gameTaskFuture;
    private SaveManager saveManager;
    // 全局校验

    public Vote vote;

    @Override
    public String toString() {
        return MessageFormat.format(
                "NetworkRoom [id = {0}, isGaming = {1}, isPaused = {2}, currentStep = {3}, stepRate = {4}]",
                roomId, isGaming, isPaused, currentStep, stepRate);
    }

    public ServerRoom(int id) {
        // 指定房间id
        roomId = id;
        // 初始化玩家控制器，连接控制器，和存档管理器
        playerManager = new PlayerManager(this, Rukkit.getConfig().maxPlayer);
        connectionManager = new RoomConnectionManager(this);
        saveManager = new SaveManager(this);
        config = Rukkit.getRoundConfig();
        vote = new Vote(this);
    }

    public class CheckSumTask implements Runnable {
        Logger log = LoggerFactory.getLogger("CheckSum Task Room #" + roomId);

        // public void check(int recheck) {
        // // recheck 失败，同步游戏
        // if (recheck >= 3) {
        // log.error("Checksum failed!May be a resync needed!");
        // syncGame();
        // return;
        // }
        // // 随机选取玩家的checklist进行对比
        // CheckSumList list = null;
        // int diffcount = 0;
        // AtomicInteger time = new AtomicInteger();
        // HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
        // for (ServerRoomConnection r : connectionManager.getConnections()) {
        // if (r.checkSumSent) {
        // map.put(r.lastSyncTick, map.getOrDefault(r.lastSyncTick, 0) + 1);
        // }
        // }
        // AtomicInteger max = new AtomicInteger();
        // map.forEach((k, v) -> {
        // if (v > max.get()) {
        // max.set(v);
        // time.set(k);
        // }
        // });
        // // log.info("Max sync time: {}", time);
        // for (ServerRoomConnection r : connectionManager.getConnections()) {
        // // 未发送不检查
        // if (!r.checkSumSent)
        // continue;
        // // 随机挑选一位玩家的checkList做样本进行检查
        // if (list == null) {
        // int rnd = new Random().nextInt(connectionManager.size());
        // if (connectionManager.getConnections().get(rnd).checkSumSent)
        // list = connectionManager.getConnections().get(rnd).player.checkList;
        // else
        // continue;
        // }
        // // 同步时间检查，若不符合正确的 tick 则不检查
        // if (time.get() != r.lastSyncTick)
        // continue;
        // // 检查所有数据, 出现问题则diffcount++;
        // if (!list.checkData(r.player.checkList)) {
        // diffcount++;
        // }
        // }
        // if (diffcount >= Math.ceil(connectionManager.size() / 2.0) &&
        // connectionManager.size() >= 2) {
        // log.warn("diffcount {} > {} players!Do recheck!", diffcount,
        // Math.ceil(connectionManager.size() / 2.0));
        // check(recheck + 1);
        // // 如果有 2 人以上不同步则执行游戏同步
        // } else if (diffcount >= 2) {
        // log.info("Desync found.Resyncing game...");
        // syncGame();
        // } else {
        // log.info("Checksum complete!");
        // }
        // for (ServerRoomConnection r : connectionManager.getConnections()) {
        // r.checkSumSent = false;
        // }
        // log.info("diffcount: {}, maxSyncTime: {}", diffcount, time);
        // }

        /**
         * 执行新的同步检查
         * 
         * @param recheck       重新检查标志
         * @param playerManager 玩家管理器
         * @return 可信玩家列表，用于同步状态
         */
        public void newCheck(int recheck, PlayerManager playerManager) {
            NetworkPlayer[] players = playerManager.getPlayerArray();
            List<NetworkPlayer> validPlayers = new ArrayList<>();

            // 将数组转换为List以便处理
            List<NetworkPlayer> playerList = new ArrayList<>();
            for (NetworkPlayer player : players) {
                if (player != null && !player.isEmpty) {
                    playerList.add(player);
                }
            }

            // 只有一个玩家 -> 对比啥呢 它2^999金币我都不管 ret
            if (playerList.size() <= 1) {
                // log.debug("Only one player, no need to check sync");
                return;
            }

            // 检查相同 lastSyncTick 不同则log
            Map<Long, List<NetworkPlayer>> tickGroups = new HashMap<>();
            for (NetworkPlayer player : playerList) {
                long lastSyncTick = player.getConnection().lastSyncTick;
                tickGroups.computeIfAbsent(lastSyncTick, k -> new ArrayList<>()).add(player);

                // 记录不同步的tick
                if (tickGroups.size() > 1) {
                    log.warn("Player {} has different lastSyncTick: {}", player.name, lastSyncTick);
                }
            }

            // 只有两个玩家
            if (playerList.size() == 2) {
                NetworkPlayer p1 = playerList.get(0);
                NetworkPlayer p2 = playerList.get(1);

                // 直接对比chksumList
                boolean isSync = p1.checkList.checkData(p2.checkList);
                if (!isSync) {
                    log.warn("Two players desync detected between {} and {}", p1.name, p2.name);
                    // 有异常则提示玩家（对比了其实也没用）
                    // 暂时不完成具体提示逻辑
                }
                return;
            }

            // 玩家>=3
            if (playerList.size() >= 3) {
                // 分组统计checksum结果
                Map<String, List<NetworkPlayer>> checksumGroups = new HashMap<>();

                for (NetworkPlayer player : playerList) {
                    // 生成checksum的字符串表示用于分组
                    StringBuilder checksumKey = new StringBuilder();
                    for (ChecksumItem item : player.checkList.getCheckList()) {
                        checksumKey.append(item.getCheckData()).append(":");
                    }
                    String key = checksumKey.toString();

                    checksumGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(player);
                }

                // 全部相同 great 什么都不用干 ret
                if (checksumGroups.size() == 1) {
                    log.debug("All players are in sync");
                    validPlayers.addAll(playerList);
                    return;
                }

                // 全部不同 提示异常
                if (checksumGroups.size() == playerList.size()) {
                    log.error("All players have different checksums! Serious desync detected!");
                    // 这种情况下可以选择信任服务器或第一个玩家
                    if (!playerList.get(0).isEmpty) {
                        validPlayers.add(playerList.get(0));
                    }
                    // TODO: unfinish
                    return;
                }

                // 处理分组情况
                List<List<NetworkPlayer>> groups = new ArrayList<>(checksumGroups.values());

                // 按组大小排序（从大到小）
                groups.sort((a, b) -> Integer.compare(b.size(), a.size()));

                // 检查是否有多个最大组（大小相同）
                int maxGroupSize = groups.get(0).size();
                List<List<NetworkPlayer>> maxGroups = new ArrayList<>();

                for (List<NetworkPlayer> group : groups) {
                    if (group.size() == maxGroupSize) {
                        maxGroups.add(group);
                    } else {
                        break; // 由于已排序，后面的组会更小
                    }
                }

                // 如果只有一个最大组，从众选择
                if (maxGroups.size() == 1) {
                    validPlayers.addAll(maxGroups.get(0));
                    log.warn("Desync detected, selected majority group with {} players", maxGroups.get(0).size());
                    // return validPlayers;
                    // TODO: unfinish
                    syncGame(validPlayers);
                    return;
                }

                // 多个最大组（如2-2、2-2-1、2-2-2、3-3-2等情况）
                if (maxGroups.size() >= 2) {
                    // 随机选择一个最大组
                    int randomIndex = (int) (Math.random() * maxGroups.size());
                    List<NetworkPlayer> selectedGroup = maxGroups.get(randomIndex);
                    validPlayers.addAll(selectedGroup);

                    // 记录具体的分组情况
                    StringBuilder groupInfo = new StringBuilder();
                    for (int i = 0; i < groups.size(); i++) {
                        if (i > 0)
                            groupInfo.append("-");
                        groupInfo.append(groups.get(i).size());
                    }

                    log.warn("Multiple max groups detected ({}), randomly selected group with {} players",
                            groupInfo.toString(), selectedGroup.size());
                    // return validPlayers;
                    syncGame(validPlayers);
                    return;
                }
            }

            return;// wtf? 理论不会被执行
        }

        @Override
        public void run() {
            // checkSumReceived意为应答sync checksum的连接(client)数量
            if (checkRequested) {
                synchronized (checkSumReceived) {
                    while (true) {
                        try {
                            checkSumReceived.wait();// 等待应答
                            if (checkSumReceived.get() >= connectionManager.size()) {
                                // 当连接全部都已应答时开始校验
                                break;
                            }
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                newCheck(0, playerManager);
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
                        doChecksum();
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
                // log.debug("tick:" + tickTime);
                try {
                    if (commandQuere.isEmpty() && !isPaused) {
                        connectionManager.broadcast(UniversalPacket.emptyCommand(currentStep));
                    } else {
                        while (!commandQuere.isEmpty() && !isPaused) {
                            NewGameCommand cmd = commandQuere.removeLast();
                            connectionManager.broadcast(UniversalPacket.gameCommand(currentStep, cmd));
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
                // Add tickTime
                currentStep += 10;
            }

            // If playercount == 1 then have a sync and pauseGame;
            if (connectionManager.size() == 1 && !cfg.singlePlayerMode && !isPaused) {
                connectionManager.broadcastServerMessage("1 player left.We will have a sync and pause game...");
                // syncGame();
                // TODO: unfinish
                /*
                 * synchronized (threadLock) {
                 * try {
                 * threadLock.wait();
                 * } catch (InterruptedException e) {
                 * e.printStackTrace();
                 * }
                 * }
                 */
                setPaused(true);
                return;
            }

            /*
             * If playerCount == 0 then pauseGame
             */
            if (connectionManager.size() <= 0) {
                /*
                 * synchronized (threadLock) {
                 * try {
                 * threadLock.wait();
                 * } catch (InterruptedException e) {
                 * e.printStackTrace();
                 * }
                 * }
                 */
                setPaused(true);
                return;
            }

            // If using query mode:
            synchronized (commandQuere) {
                // log.debug("tick:" + tickTime);
                try {
                    if (commandQuere.isEmpty() && !isPaused) {
                        connectionManager.broadcast(UniversalPacket.emptyCommand(currentStep));
                    } else {
                        while (!commandQuere.isEmpty() && !isPaused) {
                            NewGameCommand cmd = commandQuere.removeLast();
                            connectionManager.broadcast(UniversalPacket.gameCommand(currentStep, cmd));
                        }
                    }
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Game Sync task.
     */
    public class SyncTask implements Runnable {
        private List<NetworkPlayer> validPlayers = null;

        public void setValidPlayer(List<NetworkPlayer> validPlayers) {
            this.validPlayers = validPlayers;
        }

        @Override
        public void run() {
            Logger log = LoggerFactory.getLogger("SyncTask #" + roomId);
            connectionManager.clearAllSaveData();
            setPaused(true);
            // 自定义地图的同步逻辑
            try {
                // Rukkit.getSaveManager().sendDefaultSaveToAll();
                // Rukkit.getConnectionManager().broadcast(Packet.syncCheckSum());

                broadcast(UniversalPacket.chat("SERVER", "正在同步游戏", -1));
                connectionManager.broadcast(UniversalPacket.sendPullSave(ServerRoom.this));
                SaveData save;
                long time = System.currentTimeMillis();
                while (true) {
                    save = connectionManager.getAvailableSave(validPlayers);// 提供可信玩家列表
                    if (save != null) {
                        saveManager.setLastSave(save);
                        saveManager.sendLastSaveToAll(false);

                        // save.loadSave();
                        // tickTime = save.time;
                        setPaused(false);
                        connectionManager.broadcastServerMessage("同步成功");
                        for (NetworkPlayer player : validPlayers) {
                            connectionManager.broadcastServerMessage("同步信任玩家[" + player.name + "]");
                        }
                        break;
                    } else if (System.currentTimeMillis() - time > 5000) {
                        log.warn("Sync failed!");
                        connectionManager.broadcastServerMessage("同步失败!!!!!");
                        connectionManager.broadcastServerMessage("同步失败!!!!!");
                        connectionManager.broadcastServerMessage("同步失败!!!!!");
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
        //检查是否已经在检查中了
        if (!checkRequested) {
            checkSumReceived.set(0);
            checkRequested = true;
            for (ServerRoomConnection r : connectionManager.connections) {
                r.doChecksum();
            }
            Rukkit.getThreadManager().submit(new CheckSumTask());
        } else {
            // 如果客户端没有在下一次检查前应答校验包
            // 立刻开始检查已经发送的人员而不是重新进行
            checkSumReceived.set(connectionManager.size());
            synchronized (checkSumReceived) {
                checkSumReceived.notify();
            }
        }
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
                connectionManager.broadcast(UniversalPacket.packetReturnToBattleroom());
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
        // Rukkit.getThreadManager().shutdown();
    }

    /**
     * Broadcast a packet.
     * 
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
        // if (Rukkit.getConfig().nonStopMode) {
        // return true;
        // }
        if (currentStep <= 0) {
            isGaming = false;
        } else {
            isGaming = true;
        }
        return isGaming;
    }

    public void syncGame(List<NetworkPlayer> validPlayers) {
        SyncTask task = new SyncTask();
        task.setValidPlayer(validPlayers);
        Rukkit.getThreadManager().submit(task);
    }

    /**
     * starts a round game.
     */
    public void startGame() {
        try {
            connectionManager.broadcast(UniversalPacket.gameStart());

            // 载入tmx文件到Virtual World
            world = new VirtualWorld(this);
            if (Rukkit.getRoundConfig().mapType == 0) {
                MapParser m = new MapParser(
                        Rukkit.getEnvPath() + "/data/maps/skirmish/" + Rukkit.getRoundConfig().mapName + ".tmx");
                world.units = m.getMapInfo().units;

                // 第一步 仅保留有效玩家的Unit
                List<Integer> validIndex = new ArrayList<>();
                for (NetworkPlayer currPlayer : playerManager.getPlayerArray()) {
                    if (!currPlayer.isEmpty) {
                        validIndex.add(currPlayer.playerIndex);

                    }
                }
                for (int i = 0; i < world.units.size(); i++) {
                    boolean isValid = false;
                    for (int cur : validIndex) {
                        if (world.units.get(i).index == cur) {
                            isValid = true;
                        }
                    }
                    if (!isValid) {
                        world.units.remove(i);
                        i--;
                    }
                }
                for (int i = 0; i < world.units.size(); i++) {
                    world.units.get(i).id = i + 1;
                }
            } else if (Rukkit.getRoundConfig().mapType == 1) {
                // TODO: VWorld未支持自定义Map
                MapParser m = new MapParser(Rukkit.getEnvPath() + Rukkit.getRoundConfig().mapName + ".tmx");
            }

            // Set shared control.
            if (Rukkit.getRoundConfig().sharedControl) {
                for (NetworkPlayer p : playerManager.getPlayerArray()) {
                    try {
                        p.isNull();
                        p.isSharingControl = false;
                    } catch (NullPointerException ignored) {
                        continue;
                    }
                }
            }
            // Reset tick time
            currentStep = 0;
            // Broadcast start packet.
            connectionManager.broadcast(UniversalPacket.serverInfo(config));
            for (ServerRoomConnection conn : connectionManager.getConnections()) {
                conn.updateTeamList();
            }
            gameTaskFuture = Rukkit.getThreadManager().schedule(new GameTask(), stepRate, stepRate);
            // connectionManager.broadcast()
            isGaming = true;
            RoomStartGameEvent.getListenerList().callListeners(new RoomStartGameEvent(this));
        } catch (IOException ignored) {
        }
    }

    /**
     * Start a no-stop game.
     * only works if nonStop config == true
     */
    // public void startNoStopGame() {
    // if (Rukkit.getConfig().nonStopMode) {
    // gameTaskFuture = Rukkit.getThreadManager().schedule(new NonStopGameTask(),
    // stepRate, stepRate);
    // }
    // }

    public void changeMapWhileRunning(String mapName, int type) {
        Rukkit.getRoundConfig().mapName = mapName;
        Rukkit.getRoundConfig().mapType = type;
        try {
            connectionManager.broadcast(UniversalPacket.gameStart());
            // Set shared control.
            if (Rukkit.getRoundConfig().sharedControl) {
                for (NetworkPlayer p : playerManager.getPlayerArray()) {
                    try {
                        p.isNull();
                        // p.isSharingControl = true;
                    } catch (NullPointerException ignored) {
                        continue;
                    }
                }
            }
            // Reset tick time
            currentStep = 0;
            // Broadcast start packet.
            connectionManager.broadcast(UniversalPacket.serverInfo(config));
            for (ServerRoomConnection conn : connectionManager.getConnections()) {
                conn.updateTeamList(false);
            }
        } catch (IOException ignored) {
        }
    }

    public void notifyGameTask() {
        // hreadLock.notify();
        setPaused(false);
    }

    public int getTickTime() {
        return currentStep;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public void addCommand(NewGameCommand cmd) {
        if (Rukkit.getConfig().useCommandQuere) {
            commandQuere.addLast(cmd);
        } else {
            try {
                broadcast(UniversalPacket.gameCommand(this.currentStep, cmd));
            } catch (IOException ignored) {
            }
        }
    }

    // 实用方法
    public void summonUnit(String unitName, float x, float y, int player) {
        try {
            broadcast(UniversalPacket.gameSummon(getCurrentStep(), unitName, x, y, player));
        } catch (IOException e) {

        }
    }

    public void summonUnit(String unitName, float x, float y) {
        try {
            broadcast(UniversalPacket.gameSummon(getCurrentStep(), unitName, x, y));
        } catch (IOException e) {

        }
    }
}
