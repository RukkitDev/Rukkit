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
import cn.rukkit.game.NetworkPlayer;
import cn.rukkit.game.SaveData;
import cn.rukkit.network.command.GameCommand;
import cn.rukkit.network.core.handler.ServerConnectionHandler;
import cn.rukkit.network.core.packet.Packet;
import cn.rukkit.network.core.packet.PacketType;
import cn.rukkit.network.core.packet.UniversalPacket;
import cn.rukkit.network.io.GameOutputStream;
import cn.rukkit.network.io.GzipEncoder;
import cn.rukkit.util.GameUtils;

import java.io.IOException;
import java.util.concurrent.ScheduledFuture;

/**
 * Master-compatible counterpart of the legacy RoomConnection.
 *
 * <p>The command type intentionally remains {@link GameCommand}; command
 * parsing is outside this migration step.</p>
 */
public class ServerRoomConnection {
    public NetworkPlayer player;
    public ServerConnectionHandler handler;
    public ServerRoom currectRoom;
    public long pingTime;
    public int lastSyncTick = 0;
    public boolean checkSumSent = false;
    public int numberOfDesyncError = 0;
    public volatile SaveData save;

    private ScheduledFuture<?> pingFuture;
    private ScheduledFuture<?> teamFuture;

    public ServerRoomConnection(ServerConnectionHandler handler, ServerRoom currectRoom) {
        this.handler = handler;
        this.currectRoom = currectRoom;
    }

    public void startPingTask() {
        if (pingFuture != null) {
            return;
        }
        pingFuture = Rukkit.getThreadManager().schedule(new PingTasker(), 2000, 2000);
    }

    public void startTeamTask() {
        if (teamFuture != null) {
            return;
        }
        teamFuture = Rukkit.getThreadManager().schedule(new TeamTasker(), 1000, 1000);
    }

    public void stopPingTask() {
        if (pingFuture == null) {
            return;
        }
        Rukkit.getThreadManager().shutdownTask(pingFuture);
        pingFuture = null;
    }

    public void stopTeamTask() {
        if (teamFuture == null) {
            return;
        }
        Rukkit.getThreadManager().shutdownTask(teamFuture);
        teamFuture = null;
    }

    public class PingTasker implements Runnable {
        @Override
        public void run() {
            try {
                sendPacket(UniversalPacket.ping());
                pingTime = System.currentTimeMillis();
            } catch (IOException e) {
                stopPingTask();
            }
        }
    }

    public class TeamTasker implements Runnable {
        @Override
        public void run() {
            try {
                updateTeamList();
            } catch (IOException e) {
                stopTeamTask();
            }
        }
    }

    public void doChecksum() {
        try {
            sendPacket(UniversalPacket.syncCheckSum(lastSyncTick));
        } catch (IOException ignored) {
        }
    }

    public void sendChat(String msg) {
        try {
            currectRoom.connectionManager.broadcast(
                    UniversalPacket.chat(player.name, msg, player.playerIndex));
        } catch (IOException ignored) {
        }
    }

    public void sendServerMessage(String msg) {
        try {
            sendPacket(UniversalPacket.chat("SERVER", msg, -1));
        } catch (IOException ignored) {
        }
    }

    public void sendMessage(String from, String msg, int team) {
        try {
            sendPacket(UniversalPacket.chat(from, msg, team));
        } catch (IOException ignored) {
        }
    }

    public void sendGameCommand(GameCommand cmd) {
        if (currectRoom.isPaused()) {
            return;
        }
        if (Rukkit.getConfig().useCommandQuere) {
            currectRoom.addCommand(cmd);
        } else {
            try {
                currectRoom.connectionManager.broadcast(
                        UniversalPacket.gameCommand(currectRoom.getTickTime(), cmd));
            } catch (IOException ignored) {
            }
        }
    }

    public void updateTeamList() throws IOException {
        updateTeamList(currectRoom.isGaming());
    }

    public void updateTeamList(boolean simpleMode) throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeInt(player.playerIndex);
        output.writeBoolean(simpleMode);
        output.writeInt(Rukkit.getConfig().maxPlayer);
        GzipEncoder encoder = output.getEncodeStream("teams", true);

        for (int i = 0; i < Rukkit.getConfig().maxPlayer; i++) {
            NetworkPlayer currentPlayer = currectRoom.playerManager.get(i);
            encoder.stream.writeBoolean(!currentPlayer.isEmpty);
            if (currentPlayer.isEmpty) {
                continue;
            }
            encoder.stream.writeInt(255);
            currentPlayer.writePlayer(encoder.stream, simpleMode);
        }
        output.flushEncodeData(encoder);
        output.writeInt(currectRoom.config.fogType);
        output.writeInt(GameUtils.getMoneyFormat(currectRoom.config.credits));
        output.writeBoolean(true);
        output.writeInt(1);
        output.writeByte(4);
        output.writeInt(250);
        output.writeInt(250);
        output.writeInt(currectRoom.config.startingUnits);
        output.writeFloat(currectRoom.config.income);
        output.writeBoolean(currectRoom.config.disableNuke);
        output.writeBoolean(false);
        output.writeBoolean(false);
        output.writeBoolean(currectRoom.config.sharedControl);
        sendPacket(output.createPacket(PacketType.TEAM_LIST));
    }

    public void kick(String reason) {
        try {
            sendPacket(UniversalPacket.kick(reason));
        } catch (IOException ignored) {
        }
    }

    public void sendPacket(Packet packet) {
        handler.ctx.writeAndFlush(packet);
    }

    public void pong() {
        if (player == null) {
            return;
        }
        player.ping = (int) (System.currentTimeMillis() - pingTime);
        player.recordHeartbeat();
    }
}
