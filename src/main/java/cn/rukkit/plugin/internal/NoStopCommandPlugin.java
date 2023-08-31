/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.plugin.internal;

import cn.rukkit.Rukkit;
import cn.rukkit.command.ChatCommand;
import cn.rukkit.command.ChatCommandListener;
import cn.rukkit.command.CommandManager;
import cn.rukkit.game.NetworkPlayer;
import cn.rukkit.network.NetworkRoom;
import cn.rukkit.network.RoomConnection;
import cn.rukkit.network.RoomConnectionManager;
import cn.rukkit.network.packet.Packet;
import cn.rukkit.plugin.PluginConfig;
import cn.rukkit.util.LangUtil;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

public class NoStopCommandPlugin extends InternalRukkitPlugin{
    // 投票类
    public class Vote{
        // 投票id
        int voteId;
        // 房间实例
        NetworkRoom room;
        private int agree = 0;
        private int disagree = 0;
        private int timeRemain = 15;
        private ScheduledFuture voteFuture;
        private String voteDesc = "<blank>";
        boolean[] voteState;
        boolean isVoting = false;

        public boolean submitVoting(final Runnable runnable, String reason, int timeRem) {
            if (isVoting) {
                return false;
            }
            RoomConnectionManager con = room.connectionManager;
            con.broadcastServerMessage(reason);
            timeRemain = timeRem;
            voteDesc = reason;
            voteFuture = Rukkit.getThreadManager().schedule(
                    new Runnable() {
                        @Override
                        public void run() {
                            // No player exists.Stop vote.
                            if (room.connectionManager.size() <= 0) {
                                stopVote();
                            }
                            if (timeRemain == 0) {
                                if (agree >= disagree) {
                                    con.broadcastServerMessage(
                                            MessageFormat.format(LangUtil.getString("nostop.vote.success"), agree, disagree));
                                    runnable.run();
                                } else {
                                    con.broadcastServerMessage(
                                            MessageFormat.format(LangUtil.getString("nostop.vote.failure"), agree, disagree));
                                }
                                stopVote();
                            }
                            if (timeRemain % 10 == 0) {
                                con.broadcastServerMessage(MessageFormat.format(LangUtil.getString("nostop.vote.timeRemain"), timeRemain));
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

        void stopVote() {
            isVoting = false;
            agree = disagree = 0;
            Arrays.fill(voteState, false);
            Rukkit.getThreadManager().shutdownTask(voteFuture);
        }
    }

    public class AgreeCallback implements ChatCommandListener {
        @Override
        public boolean onSend(RoomConnection con, String[] args) {
            Vote vote = voteList.get(con.currectRoom);
            if (vote.isVoting) {
                if (vote.agree(con.player.playerIndex)) {
                    con.sendServerMessage(LangUtil.getString("nostop.vote.submit"));
                } else {
                    con.sendServerMessage(LangUtil.getString("nostop.vote.alreadySubmit"));
                }
            } else {
                con.sendServerMessage(LangUtil.getString("nostop.vote.noCurrentVote"));
            }
            return false;
        }
    }



    public class DisagreeCallback implements ChatCommandListener {
        @Override
        public boolean onSend(RoomConnection con, String[] args) {
            Vote vote = voteList.get(con.currectRoom);
            if (vote.isVoting) {
                if (vote.disagree(con.player.playerIndex)) {
                    con.sendServerMessage(LangUtil.getString("nostop.vote.submit"));
                } else {
                    con.sendServerMessage(LangUtil.getString("nostop.vote.alreadySubmit"));
                }
            } else {
                con.sendServerMessage(LangUtil.getString("nostop.vote.noCurrentVote"));
            }
            return false;
        }
    }

    class NukeCallback implements ChatCommandListener {
        @Override
        public boolean onSend(RoomConnection con, final String[] args) {
            Vote vote = voteList.get(con.currectRoom);
            if (args.length < 1) {
                // Do nothing.
            } else {
                boolean result = vote.submitVoting(new Runnable() {
                                                       @Override
                                                       public void run() {
                                                           Rukkit.getRoundConfig().disableNuke = !Boolean.parseBoolean(args[0]);
                                                           try {
                                                               con.currectRoom.broadcast(Packet.serverInfo(con.currectRoom.config));
                                                           } catch (IOException ignored) {}
                                                       }
                                                   },
                        MessageFormat.format(LangUtil.getString("nostop.vote.nukes"), con.player.name, (Boolean.parseBoolean(args[0]) ? "启用":"禁用")),
                        30);
                if (!result) con.sendServerMessage(LangUtil.getString("nostop.vote.voteExist"));

            }
            return false;
        }
    }

    class IncomeCallback implements ChatCommandListener {
        float income = 0;
        @Override
        public boolean onSend(RoomConnection con, String[] args) {
            Vote vote = voteList.get(con.currectRoom);
            if (args.length == 0) {
                // Do nothing.
            } else {
                income = Float.parseFloat(args[0]);
                if (income > 100 && income < 0) {
                    income = 1;
                }
                boolean result = vote.submitVoting(new Runnable() {
                                                       @Override
                                                       public void run() {
                                                           try {
                                                               con.currectRoom.config.income = income;
                                                               con.currectRoom.broadcast(Packet.serverInfo(con.currectRoom.config));
                                                               con.currectRoom.syncGame();
                                                           } catch (IOException e) {}
                                                       }
                                                   },
                        MessageFormat.format(LangUtil.getString("nostop.vote.income"), con.player.name, income),
                        30);
                if (!result) con.sendServerMessage(LangUtil.getString("nostop.vote.voteExist"));
            }
            return false;
        }
    }

    class SyncCallback implements ChatCommandListener {
        @Override
        public boolean onSend(RoomConnection con, String[] args) {
            Vote vote = voteList.get(con.currectRoom);
            boolean result = vote.submitVoting(new Runnable() {
                                                   @Override
                                                   public void run() {
                                                       con.currectRoom.syncGame();
                                                   }
                                               },
                    MessageFormat.format(LangUtil.getString("nostop.vote.sync"), con.player.name),
                    30);
            if (!result) con.sendServerMessage(LangUtil.getString("nostop.vote.voteExist"));
            return false;
        }
    }

    public HashMap<NetworkRoom, Vote> voteList = new HashMap<NetworkRoom, Vote>();

    @Override
    public void onLoad() {
        CommandManager mgr = Rukkit.getCommandManager();
        mgr.registerCommand(new ChatCommand("help", LangUtil.getString("chat.help"), 1, new CommandPlugin.HelpCallback(), this));
        mgr.registerCommand(new ChatCommand("state", LangUtil.getString("chat.state"), 0, new CommandPlugin.StateCallback(), this));
        mgr.registerCommand(new ChatCommand("version", LangUtil.getString("chat.version"), 0, new CommandPlugin.VersionCallback(), this));
        //mgr.registerCommand(new ChatCommand("team", "Send a team message.", 1, new TeamChatCallback(), this));
        mgr.registerCommand(new ChatCommand("t", LangUtil.getString("chat.t"), 1, new CommandPlugin.TeamChatCallback(), this));
        mgr.registerCommand(new ChatCommand("maps", LangUtil.getString("chat.maps"), 1, new CommandPlugin.MapsCallback(0), this));
        mgr.registerCommand(new ChatCommand("cmaps", LangUtil.getString("chat.cmaps"), 1, new CommandPlugin.CustomMapsCallback(0), this));
        mgr.registerCommand(new ChatCommand("y", LangUtil.getString("nostop.y"), 0, new AgreeCallback(), this));
        mgr.registerCommand(new ChatCommand("n", LangUtil.getString("nostop.n"), 0, new DisagreeCallback(), this));
        mgr.registerCommand(new ChatCommand("income", LangUtil.getString("chat.income"), 1, new IncomeCallback(), this));
        mgr.registerCommand(new ChatCommand("nukes", LangUtil.getString("chat.nukes"), 1, new NukeCallback(), this));
        mgr.registerCommand(new ChatCommand("income", LangUtil.getString("chat.income"), 1, new IncomeCallback(), this));
        for (NetworkRoom networkRoom: Rukkit.getRoomManager().roomList) {
            voteList.put(networkRoom, new Vote(networkRoom));
        }
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }

    @Override
    public void onStart() {

    }

    @Override
    public void onDone() {

    }

    @Override
    public void loadConfig() {
        config = new PluginConfig();
        config.apiVersion = "ANY";
        config.author = "Rukkit";
        config.version = "1.0.0";
        config.pluginClass = "cn.rukkit.plugin.internal.NoStopPlugin";
        config.name = "NoStopPlugin";
    }
}
