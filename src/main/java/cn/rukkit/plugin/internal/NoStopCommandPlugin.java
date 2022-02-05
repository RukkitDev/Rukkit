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
import cn.rukkit.config.RoundConfig;
import cn.rukkit.event.EventHandler;
import cn.rukkit.event.EventListener;
import cn.rukkit.event.player.PlayerJoinEvent;
import cn.rukkit.game.NetworkPlayer;
import cn.rukkit.game.PingType;
import cn.rukkit.game.PlayerManager;
import cn.rukkit.game.map.CustomMapLoader;
import cn.rukkit.game.map.OfficialMap;
import cn.rukkit.network.Connection;
import cn.rukkit.network.ConnectionManager;
import cn.rukkit.network.packet.Packet;
import cn.rukkit.plugin.PluginConfig;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.rukkit.util.LangUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ScheduledFuture;
import java.util.Arrays;

public class NoStopCommandPlugin extends CommandPlugin implements EventListener {

    Logger log = LoggerFactory.getLogger(NoStopCommandPlugin.class);

	@EventHandler
	public void playerJoin(PlayerJoinEvent event) {
		if (vote.isVoting) {
			event.getPlayer().getConnection().sendServerMessage(MessageFormat.format(LangUtil.getString("nostop.vote.joinMessage"), vote.voteDesc));
		}
	}

	public void updateDetailedTeamList() {
		for (Connection conn: Rukkit.getConnectionManager().getConnections()) {
			try {
				conn.updateTeamList(false);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public class Vote {
		private boolean isVoting = false;
		private int agree = 0;
		private int disagree = 0;
		private int timeRemain = 15;
		private ScheduledFuture voteFuture;
		private String voteDesc = "<blank>";
		private ConnectionManager con = Rukkit.getConnectionManager();
		boolean[] voteState;
		
		public Vote() {
			voteState = new boolean[Rukkit.getConfig().maxPlayer];
			Arrays.fill(voteState, false);
		}
		
		public boolean submitVotiog(final Runnable runnable, String reason, int timeRem) {
			if (isVoting) {
				return false;
			} else {
				con.broadcastServerMessage(reason);
				timeRemain = timeRem;
				voteDesc = reason;
				voteFuture = Rukkit.getThreadManager().schedule(
					new Runnable() {
						@Override
						public void run() {
							// No player exists.Stop vote.
							if (Rukkit.getConnectionManager().size() <= 0) {
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
					}
				, 1000, 1000);
				isVoting = true;
				return true;
			}
		}
		
		public void stopVote() {
			isVoting = false;
			agree = disagree = 0;
			Arrays.fill(voteState, false);
			Rukkit.getThreadManager().shutdownTask(voteFuture);
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
	}
	Vote vote = new Vote();
	
	public class AgreeCallback implements ChatCommandListener {
		@Override
		public boolean onSend(Connection con, String[] args) {
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
		public boolean onSend(Connection con, String[] args) {
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
	
	public class VersionCallBack implements ChatCommandListener {
		@Override
		public boolean onSend(Connection con, String[] args) {
			// TODO: Implement this method
			StringBuilder build = new StringBuilder();
			build.append("Rukkit Server v" + Rukkit.RUKKIT_VERSION + "\n");
			build.append("Rukkit Plugin API v" + Rukkit.PLUGIN_API_VERSION);
			build.append("Server is running on no-stop mode.");
			con.sendServerMessage(build.toString());
			return false;
		}
	}

	public class MapsCallback implements ChatCommandListener {
		private final int type;
		String mapname = "";
		public MapsCallback(int type) {
			this.type = type;
		}
		@Override
		public boolean onSend(Connection con, String[] args) {
			// TODO: Implement this method
			// Maps
			if (type == 0) {
				StringBuilder build = new StringBuilder();
				if (args.length > 0) {
					build.append("- Maps -  Page ").append(args[0]).append(" \n");
					int page = Integer.parseInt(args[0]) - 1;
					for (int i = page * 10;i < OfficialMap.maps.length;i++) {
						if (i > page * 10 + 10) break;
						build.append(String.format("[%d] %s", i, OfficialMap.maps[i])).append("\n");
					}
				} else {
					build.append("- Help -  Page 1 \n");
					for (int i = 0;i < 10;i++) {
						build.append(String.format("[%d] %s", i, OfficialMap.maps[i])).append("\n");
					}
				}
				con.sendServerMessage(build.toString());
			} else {
				if (args.length > 0) {
					if (args[0].startsWith("'")) {
						String mapString = args[0].split("'")[1];
						for (int i=0;i < OfficialMap.mapsName.length;i++) {
							if (OfficialMap.mapsName[i].contains(mapString)) {
								mapname = OfficialMap.maps[i];
								break;
							}
						}
					} else {
						int id = Integer.parseInt(args[0]);
						mapname = OfficialMap.maps[id];
					}
					boolean result = vote.submitVotiog(new Runnable() {
							@Override
							public void run() {
								Rukkit.getGameServer().changeMapWhileRunning(mapname, 0);
							}
					},
					MessageFormat.format(LangUtil.getString("nostop.vote.map"), con.player.name, mapname),
					30);
					if (!result) con.sendServerMessage(LangUtil.getString("nostop.vote.voteExist"));
				}
			}
			return false;
		}
	}

	public class CustomMapsCallback implements ChatCommandListener {
		private final int type;
		public CustomMapsCallback(int type) {
			this.type = type;
		}
		@Override
		public boolean onSend(Connection con, String[] args) {
			// TODO: Implement this method
			// Maps
			if (type == 0) {
				StringBuilder build = new StringBuilder();
				List<String> li = CustomMapLoader.getMapNameList();
				if (args.length > 0) {
					build.append("- CustomMaps -  Page ").append(args[0]).append(" \n");
					int page = Integer.parseInt(args[0]) - 1;
					for (int i = page * 10;i < li.size();i++) {
						if (i > page * 10 + 10) break;
						build.append(String.format("[%d] %s", i, li.get(i))).append("\n");
					}
				} else {
					build.append("- Help -  Page 1 \n");
					for (int i = 0; i < (Math.min(li.size(), 10)); i++) {
						build.append(String.format("[%d] %s", i, li.get(i))).append("\n");
					}
				}
				con.sendServerMessage(build.toString());
			} else {
				if (args.length > 0) {
					ArrayList mapList = CustomMapLoader.getMapNameList();
					int id = Integer.parseInt(args[0]);
					final String mapname = mapList.get(id).toString();
					boolean result = vote.submitVotiog(new Runnable() {
							@Override
							public void run() {
								Rukkit.getGameServer().changeMapWhileRunning(mapname, 1);
							}
					},
							MessageFormat.format(LangUtil.getString("nostop.vote.cmap"), con.player.name, mapname),
							30);
					if (!result) con.sendServerMessage(LangUtil.getString("nostop.vote.voteExist"));
				}
			}
			return false;
		}
	}

	// TODO: -move && -self-move 操作
	class MoveCallback implements ChatCommandListener {
		private int type;
		public MoveCallback(int type) {
			this.type = type;
		}
		@Override
		public boolean onSend(Connection con, String[] cmd) {
			switch (type) {
					//move
				case 0:
					if (!con.player.isAdmin || Rukkit.getGameServer().isGaming() || cmd.length < 2) {
						// Do nothing.
					} else {
						PlayerManager playerGroup = Rukkit.getConnectionManager().getPlayerManager();
						NetworkPlayer fromPlayer = playerGroup.get(Integer.parseInt(cmd[0]) - 1);
						NetworkPlayer targetPlayer = playerGroup.get(Integer.parseInt(cmd[1]) - 1);
						try {
							if (fromPlayer.movePlayer(Integer.parseInt(cmd[1]) - 1)) {
								con.sendServerMessage(LangUtil.getString("chat.moveComplete"));
							} else {
								int fromslot, toslot;
								fromslot = fromPlayer.playerIndex;
								toslot = targetPlayer.playerIndex;
								playerGroup.remove(targetPlayer);
								fromPlayer.movePlayer(toslot);
								targetPlayer.movePlayer(fromslot);
							}
						} catch (Exception e) {
							//fromPlayer.movePlayer(Integer.parseInt(cmd[1]) - 1);
							e.printStackTrace();
						}
					}
					break;
					// Self-move
				case 1:
					if (cmd.length < 1) {
						// Do nothing.
					} else {
						try {
							if (con.player.movePlayer(Integer.parseInt(cmd[0]) - 1)) {
								con.sendServerMessage(LangUtil.getString("chat.moveComplete"));
								Rukkit.getConnectionManager().broadcastServerMessage(MessageFormat.format(LangUtil.getString("nostop.move"), con.player.name, con.player.playerIndex, Integer.parseInt(cmd[0])));
								updateDetailedTeamList();
							} else {
								con.sendServerMessage(LangUtil.getString("chat.playerExist"));
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
			}
			return false;
		}
	}

	class TeamCallback implements ChatCommandListener {
		private int type;
		public TeamCallback(int type) {
			this.type = type;
		}
		@Override
		public boolean onSend(Connection con, String[] args) {
			switch (type) {
					//team
				case 0:
					if (Rukkit.getGameServer().isGaming() || !con.player.isAdmin || args.length < 2) {
						// Do nothing.
					} else {
						try {
							Rukkit.getConnectionManager().getPlayerManager()
								.get(Integer.parseInt(args[0]) - 1).team = (Integer.parseInt(args[1]) - 1);
						} catch (NullPointerException e) {
							con.sendServerMessage(LangUtil.getString("chat.playerEmpty"));
						}
					}
					break;
					//self-team
				case 1:
					if (args.length < 1) return false;
					// Never got exceptions...
					con.player.team = Integer.parseInt(args[0]) - 1;
					updateDetailedTeamList();
			}
			return false;
		}
	}

	class StartCallback implements ChatCommandListener {
		@Override
		public boolean onSend(Connection con, String[] args) {
			con.sendServerMessage("no-stop mode, server is always in game!\n" +
					"不停止模式，服务器始终在游戏中！");
			return true;
		}
	}

	class NukeCallback implements ChatCommandListener {
		@Override
		public boolean onSend(Connection con, final String[] args) {
			if (args.length < 1) {
				// Do nothing.
			} else {
				boolean result = vote.submitVotiog(new Runnable() {
						@Override
						public void run() {
							Rukkit.getRoundConfig().disableNuke = !Boolean.parseBoolean(args[0]);
							try {
								Rukkit.getConnectionManager().broadcast(Packet.serverInfo());
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
		public boolean onSend(Connection con, String[] args) {
			if (args.length == 0) {
				// Do nothing.
			} else {
				income = Float.parseFloat(args[0]);
				if (income > 100 && income < 0) {
					income = 1;
				}
				boolean result = vote.submitVotiog(new Runnable() {
						@Override
						public void run() {
							try {
								Rukkit.getRoundConfig().income = income;
								Rukkit.getConnectionManager().broadcast(Packet.serverInfo());
								Rukkit.getGameServer().syncGame();
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
        public boolean onSend(Connection con, String[] args) {
            boolean result = vote.submitVotiog(new Runnable() {
					@Override
					public void run() {
						Rukkit.getGameServer().syncGame();
					}
				},
					MessageFormat.format(LangUtil.getString("nostop.vote.sync"), con.player.name),
					30);
			if (!result) con.sendServerMessage(LangUtil.getString("nostop.vote.voteExist"));
            return false;
        }
    }




	@Override
	public void onLoad() {
		// TODO: Implement this method
		getLogger().info("CommandPlugin::onLoad()");
		CommandManager mgr = Rukkit.getCommandManager();
		mgr.registerCommand(new ChatCommand("help", LangUtil.getString("chat.help"), 1, new HelpCallback(), this));
		mgr.registerCommand(new ChatCommand("state", LangUtil.getString("chat.state"), 0, new StateCallback(), this));
		mgr.registerCommand(new ChatCommand("version", LangUtil.getString("chat.version"), 0, this, this));
		//mgr.registerCommand(new ChatCommand("team", "Send a team message.", 1, new TeamChatCallback(), this));
		mgr.registerCommand(new ChatCommand("t", LangUtil.getString("chat.t"), 1, new TeamChatCallback(), this));
		mgr.registerCommand(new ChatCommand("maps", LangUtil.getString("chat.maps"), 1, new MapsCallback(0), this));
		mgr.registerCommand(new ChatCommand("map", LangUtil.getString("chat.map"), 1, new MapsCallback(1), this));
		mgr.registerCommand(new ChatCommand("cmaps", LangUtil.getString("chat.cmaps"), 1, new CustomMapsCallback(0), this));
		mgr.registerCommand(new ChatCommand("cmap", LangUtil.getString("chat.cmap"), 1, new CustomMapsCallback(1), this));
		mgr.registerCommand(new ChatCommand("kick", LangUtil.getString("chat.kick"), 1, new KickCallBack(), this));
		mgr.registerCommand(new ChatCommand("team", LangUtil.getString("chat.team"), 2, new TeamCallback(0), this));
		mgr.registerCommand(new ChatCommand("self_team", LangUtil.getString("chat.self_team"), 1, new TeamCallback(1), this));
		mgr.registerCommand(new ChatCommand("move", LangUtil.getString("chat.move"), 2, new MoveCallback(0), this));
		mgr.registerCommand(new ChatCommand("self_move", LangUtil.getString("chat.self_move"), 2, new MoveCallback(1), this));
		mgr.registerCommand(new ChatCommand("qc", LangUtil.getString("chat.qc"), 1, new QcCallback(), this));
		mgr.registerCommand(new ChatCommand("fog", LangUtil.getString("chat.fog"), 1, new SetFogCallback(), this));
		mgr.registerCommand(new ChatCommand("nukes", LangUtil.getString("chat.nukes"), 1, new NukeCallback(), this));
		mgr.registerCommand(new ChatCommand("startingunits", LangUtil.getString("chat.startingunits"), 1, new StartingUnitCallback(), this));
		mgr.registerCommand(new ChatCommand("income", LangUtil.getString("chat.income"), 1, new IncomeCallback(), this));
		mgr.registerCommand(new ChatCommand("share", LangUtil.getString("chat.share"), 1, new ShareCallback(), this));
		mgr.registerCommand(new ChatCommand("credits", LangUtil.getString("chat.credits"), 1, new CreditsCallback(), this));
		mgr.registerCommand(new ChatCommand("start", LangUtil.getString("chat.start"), 1, new StartCallback(), this));
        mgr.registerCommand(new ChatCommand("sync", LangUtil.getString("chat.sync"), 0, new SyncCallback(), this));
		mgr.registerCommand(new ChatCommand("i", LangUtil.getString("chat.i"), 1, new InfoCallback(), this));
		mgr.registerCommand(new ChatCommand("chksum", LangUtil.getString("chat.chksum"), 0, new ChksumCallback(), this));
		mgr.registerCommand(new ChatCommand("maping", LangUtil.getString("chat.maping"), 2, new PingCallBack(), this));
		mgr.registerCommand(new ChatCommand("list", LangUtil.getString("chat.list"), 0, new PlayerListCallback(), this));
		mgr.registerCommand(new ChatCommand("surrender", LangUtil.getString("chat.surrender"), 0, new SurrenderCallback(), this));
		mgr.registerCommand(new ChatCommand("y", LangUtil.getString("nostop.y"), 0, new AgreeCallback(), this));
		mgr.registerCommand(new ChatCommand("n", LangUtil.getString("nostop.n"), 0, new DisagreeCallback(), this));
		getPluginManager().registerEventListener(this, this);
	}

	@Override
	public void onEnable() {
		// TODO: Implement this method
		getLogger().info("CommandPlugin::onEnable()");
	}

	@Override
	public void onDisable() {
		// TODO: Implement this method
	}

	@Override
	public void onStart() {
		// TODO: Implement this method
	}

	@Override
	public void onDone() {
		// TODO: Implement this method
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
