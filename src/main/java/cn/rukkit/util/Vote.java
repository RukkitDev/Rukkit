/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.util;

import cn.rukkit.Rukkit;
import cn.rukkit.network.NetworkRoom;
import cn.rukkit.network.RoomConnectionManager;
import cn.rukkit.network.room.ServerRoom;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.concurrent.ScheduledFuture;

public class Vote{
    // 投票id
    public String voteId = "null";
    // 房间实例
    NetworkRoom room;
    ServerRoom serverRoom;
    private int agree = 0;
    private int disagree = 0;
    private int timeRemain = 15;
    private ScheduledFuture voteFuture;
    public String voteDesc = "<blank>";

    boolean[] voteState;
    public boolean isVoting = false;

    public boolean disabledVote = false;

    public boolean submitVoting(final Runnable runnable, String id, String reason, int timeRem) {
        if (isVoting) {
            return false;
        }
        if (room != null) {
            room.connectionManager.broadcastServerMessage(reason);
        } else {
            serverRoom.connectionManager.broadcastServerMessage(reason);
        }
        timeRemain = timeRem;
        voteId = id;
        voteDesc = reason;
        voteFuture = Rukkit.getThreadManager().schedule(
                new Runnable() {
                    @Override
                    public void run() {
                        // No player exists.Stop vote.
                        if (connectionCount() <= 0) {
                            stopVote();
                        }
                        if (timeRemain == 0) {
                            if (agree >= disagree) {
                                broadcastServerMessage(
                                        MessageFormat.format(LangUtil.getString("nostop.vote.success"), agree, disagree));
                                runnable.run();
                            } else {
                                broadcastServerMessage(
                                        MessageFormat.format(LangUtil.getString("nostop.vote.failure"), agree, disagree));
                            }
                            stopVote();
                        }
                        if (timeRemain % 10 == 0) {
                            broadcastServerMessage(MessageFormat.format(LangUtil.getString("nostop.vote.timeRemain"), timeRemain));
                        }
                        timeRemain --;
                    }
                }, 1000, 1000);
        isVoting = true;
        return true;
    }

    public Vote(NetworkRoom room) {
        this.room = room;
        voteState = new boolean[room.playerManager.getMaxPlayer()];
    }

    public Vote(ServerRoom room) {
        this.serverRoom = room;
        voteState = new boolean[room.playerManager.getMaxPlayer()];
    }

    public boolean agree(int index) {
        if (isVoting && !voteState[index]) {
            agree++;
            voteState[index] = true;
            return true;
        }
        return false;
    }

    public boolean disagree(int index) {
        if (isVoting && !voteState[index]) {
            disagree++;
            voteState[index] = true;
            return true;
        }
        return false;
    }

    public void stopVote() {
        isVoting = false;
        agree = disagree = 0;
        disabledVote = false;
        Arrays.fill(voteState, false);
        if (voteFuture != null) Rukkit.getThreadManager().shutdownTask(voteFuture);
    }

    private int connectionCount() {
        return room != null ? room.connectionManager.size() : serverRoom.connectionManager.size();
    }

    private void broadcastServerMessage(String message) {
        if (room != null) {
            room.connectionManager.broadcastServerMessage(message);
        } else {
            serverRoom.connectionManager.broadcastServerMessage(message);
        }
    }
}
