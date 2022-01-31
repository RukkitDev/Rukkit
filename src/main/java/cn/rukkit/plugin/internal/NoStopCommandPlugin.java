/*
 *  All Rights Reserved.
 *  FileName: NoStopPlugin.java
 *  @author: wtbdev
 *  @date: 2022/1/30 下午4:37
 */

package cn.rukkit.plugin.internal;

import cn.rukkit.Rukkit;
import cn.rukkit.command.ChatCommand;
import cn.rukkit.command.ChatCommandListener;
import cn.rukkit.command.CommandManager;
import cn.rukkit.config.RoundConfig;
import cn.rukkit.event.EventListener;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ScheduledFuture;
import java.util.Arrays;

public class NoStopCommandPlugin extends CommandPlugin implements EventListener {

    Logger log = LoggerFactory.getLogger(NoStopCommandPlugin.class);

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
		public abstract class VoteRunnable implements Runnable{
			public void stopVote() {
				Vote.this.stopVote();
			}
		}
		private boolean isVoting = false;
		private int agree = 0;
		private int disagree = 0;
		private int timeRemain = 15;
		private ScheduledFuture voteFuture;
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
										String.format("同意: %d人, 反对: %d人，投票成功！", agree, disagree));
									runnable.run();
								} else {
									con.broadcastServerMessage(
										String.format("同意: %d人, 反对: %d人，投票失败！", agree, disagree));
								}
								stopVote();
							}
							if (timeRemain % 10 == 0) {
								con.broadcastServerMessage("还有" + timeRemain + "s结束投票!");
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
					con.sendServerMessage("投票成功!");
				} else {
					con.sendServerMessage("你已经投过了!");
				}
			} else {
				con.sendServerMessage("当前没有正在进行的投票!");
			}
			return false;
		}
	}
	
	public class DisagreeCallback implements ChatCommandListener {
		@Override
		public boolean onSend(Connection con, String[] args) {
			if (vote.isVoting) {
				if (vote.disagree(con.player.playerIndex)) {
					con.sendServerMessage("投票成功!");
				} else {
					con.sendServerMessage("你已经投过了!");
				}
			} else {
				con.sendServerMessage("当前没有正在进行的投票!");
			}
			return false;
		}
	}
	
	public class VersionCallBack implements ChatCommandListener {
		@Override
		public boolean onSend(Connection con, String[] args) {
			// TODO: Implement this method
			StringBuilder build = new StringBuilder();
			build.append("Rukkit 服务器 v" + Rukkit.RUKKIT_VERSION + "\n");
			build.append("插件 API v" + Rukkit.PLUGIN_API_VERSION);
			build.append("服务器运行不停止模式中.");
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
					build.append("- 地图 - 第 ").append(args[0]).append(" 页 \n");
					int page = Integer.parseInt(args[0]) - 1;
					for (int i = page * 10;i < OfficialMap.maps.length;i++) {
						if (i > page * 10 + 10) break;
						build.append(String.format("[%d] %s", i, OfficialMap.maps[i])).append("\n");
					}
				} else {
					build.append("- 地图 -  第 1 页 \n");
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
					String.format("玩家 %s 投票更换地图: %s,输入(-y/-n)来投票", con.player.name, mapname),
					30);
					if (!result) con.sendServerMessage("已经有一个正在进行的投票了!");
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
					build.append("- 自定义地图 -  第 ").append(args[0]).append("页 \n");
					int page = Integer.parseInt(args[0]) - 1;
					for (int i = page * 10;i < li.size();i++) {
						if (i > page * 10 + 10) break;
						build.append(String.format("[%d] %s", i, li.get(i))).append("\n");
					}
				} else {
					build.append("- 自定义地图 -  第 1 页 \n");
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
					String.format("玩家 %s 投票更换自定义地图: %s,输入(-y/-n)(同意/拒绝)来投票", con.player.name, mapname),
					30);
					if (!result) con.sendServerMessage("已经有一个正在进行的投票了!");
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
								con.sendServerMessage("移动成功！");
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
								con.sendServerMessage("移动成功!");
								Rukkit.getConnectionManager().broadcastServerMessage(String.format("提示：玩家 %s 从 %d 移动到 %d 上！", con.player.name, con.player.playerIndex, Integer.parseInt(cmd[0])));
								updateDetailedTeamList();
							} else {
								con.sendServerMessage("Fail: 已经有一个玩家在这个位置");
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
							con.sendServerMessage("玩家不存在!");
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
					String.format("玩家 %s 投票修改禁核为%s，输入(-y/-n)(同意/拒绝)来投票", con.player.name, (Boolean.parseBoolean(args[0]) ? "启用":"禁用")),
					30);
				if (!result) con.sendServerMessage("已经有一个正在进行的投票了!");
				
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
				if (income > 100 || income < 0) {
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
					String.format("玩家 %s 投票修改资金倍率为:%fx,输入(-y/-n)(同意/拒绝)来投票", con.player.name, income),
					30);
				if (!result) con.sendServerMessage("已经有一个正在进行的投票了!");
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
				String.format("玩家 %s 投票进行游戏同步,输入(-y/-n)(同意/拒绝)来投票", con.player.name),
				30);
			if (!result) con.sendServerMessage("已经有一个正在进行的投票了!");
            return false;
        }
    }




	@Override
	public void onLoad() {
		// TODO: Implement this method
		getLogger().info("CommandPlugin::onLoad()");
		CommandManager mgr = Rukkit.getCommandManager();
		mgr.registerCommand(new ChatCommand("help", "显示帮助.", 1, new HelpCallback(), this));
		mgr.registerCommand(new ChatCommand("state", "查看服务器状态.", 0, new StateCallback(), this));
		mgr.registerCommand(new ChatCommand("version", "查看 Rukkit 服务器版本.", 0, this, this));
		//mgr.registerCommand(new ChatCommand("team", "Send a team message.", 1, new TeamChatCallback(), this));
		mgr.registerCommand(new ChatCommand("t", "发送队伍消息.", 1, new TeamChatCallback(), this));
		mgr.registerCommand(new ChatCommand("maps", "获取官方地图列表（.maps 页数 可以换页）.", 1, new MapsCallback(0), this));
		mgr.registerCommand(new ChatCommand("map", "换图。后面跟地图序号（在maps前面的）.", 1, new MapsCallback(1), this));
		mgr.registerCommand(new ChatCommand("cmaps", "获取自定义地图列表.", 1, new CustomMapsCallback(0), this));
		mgr.registerCommand(new ChatCommand("cmap", "更换自定义地图.后面跟地图序号", 1, new CustomMapsCallback(1), this));
		mgr.registerCommand(new ChatCommand("kick", "踢出一名玩家.", 1, new KickCallBack(), this));
		mgr.registerCommand(new ChatCommand("team", "更换玩家队伍.", 2, new TeamCallback(0), this));
		mgr.registerCommand(new ChatCommand("self_team", "更换你自己的队伍.", 1, new TeamCallback(1), this));
		mgr.registerCommand(new ChatCommand("move", "给一位玩家移动位置.", 2, new MoveCallback(0), this));
		mgr.registerCommand(new ChatCommand("self_move", "移动你自己的位置（只能移到空位上）.", 2, new MoveCallback(1), this));
		mgr.registerCommand(new ChatCommand("qc", "静默执行指令.", 1, new QcCallback(), this));
		mgr.registerCommand(new ChatCommand("fog", "设置迷雾类型.", 1, new SetFogCallback(), this));
		mgr.registerCommand(new ChatCommand("nukes", "设置是否禁核（true/false).", 1, new NukeCallback(), this));
		mgr.registerCommand(new ChatCommand("startingunits", "设置初始单位.后面跟数字", 1, new StartingUnitCallback(), this));
		mgr.registerCommand(new ChatCommand("income", "设置资金倍数(1x-100x).", 1, new IncomeCallback(), this));
		mgr.registerCommand(new ChatCommand("share", "设置你自己是否共享操作.(on/off)", 1, new ShareCallback(), this));
		mgr.registerCommand(new ChatCommand("credits", "设置基础资金.", 1, new CreditsCallback(), this));
		mgr.registerCommand(new ChatCommand("start", "启动一个游戏.", 1, new StartCallback(), this));
		mgr.registerCommand(new ChatCommand("sync", "同步游戏(admin only.)", 0, new SyncCallback(), this));
		mgr.registerCommand(new ChatCommand("i", "提交不同步消息到服务器.", 1, new InfoCallback(), this));
		mgr.registerCommand(new ChatCommand("chksum", "发送checksum.", 0, new ChksumCallback(), this));
		mgr.registerCommand(new ChatCommand("maping", "Ping 地图.", 2, new PingCallBack(), this));
		mgr.registerCommand(new ChatCommand("list", "查看玩家列表.", 0, new PlayerListCallback(), this));
		mgr.registerCommand(new ChatCommand("surrender", "投降.", 0, new SurrenderCallback(), this));
		mgr.registerCommand(new ChatCommand("y", "同意当前投票.", 0, new AgreeCallback(), this));
		mgr.registerCommand(new ChatCommand("n", "拒绝当前投票.", 0, new DisagreeCallback(), this));
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
