/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.plugin.internal;
//import cn.rukkit.plugin.InternalRukkitPlugin;
import cn.rukkit.Rukkit;
import cn.rukkit.command.ChatCommand;
import cn.rukkit.command.ChatCommandListener;
import cn.rukkit.command.CommandManager;
import cn.rukkit.command.ServerCommandListener;
import cn.rukkit.config.RoundConfig;
import cn.rukkit.game.NetworkPlayer;
import cn.rukkit.game.PingType;
import cn.rukkit.game.PlayerManager;
import cn.rukkit.game.map.CustomMapLoader;
import cn.rukkit.game.map.OfficialMap;
import cn.rukkit.network.RoomConnection;
import cn.rukkit.network.RoomConnectionManager;
import cn.rukkit.network.packet.Packet;
import cn.rukkit.plugin.PluginConfig;
import cn.rukkit.util.LangUtil;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandPlugin extends InternalRukkitPlugin implements ChatCommandListener {

	//int info = 0;
	Logger log = LoggerFactory.getLogger(CommandPlugin.class);

	@Override
	public boolean onSend(RoomConnection con, String[] args) {
		// TODO: Implement this method
		StringBuilder build = new StringBuilder();
		build.append("Rukkit Server v" + Rukkit.RUKKIT_VERSION + "\n");
		build.append("Rukkit Plugin API v" + Rukkit.PLUGIN_API_VERSION);
		con.sendServerMessage(build.toString());
		return false;
	}

	static class VersionCallback implements ChatCommandListener {
		@Override
		public boolean onSend(RoomConnection con, String[] args) {
			StringBuilder build = new StringBuilder();
			build.append("Rukkit Server v" + Rukkit.RUKKIT_VERSION + "\n");
			build.append("Rukkit Plugin API v" + Rukkit.PLUGIN_API_VERSION);
			con.sendServerMessage(build.toString());
			return false;
		}
	}

	@Override
	public void loadConfig() {
		// TODO: Implement this method
		config = new PluginConfig();
		config.name = "command-plugin";
		config.author = "rukkit";
		config.version = "1.0.0";
		config.pluginClass = "cn.rukkit.plugin.internal.CommandPlugin";
		config.apiVersion = "ANY";
	}

	public class KickCallBack implements ChatCommandListener {
		@Override
		public boolean onSend(RoomConnection con, String[] args) {
			// TODO: Implement this method
			if (con.player.isAdmin && args.length > 1 || !con.currectRoom.isGaming()) {
				int id = Integer.parseInt(args[1]);
				NetworkPlayer player = con.currectRoom.playerManager.get(id);
				try {
					player.isNull();
					player.getConnection().kick(LangUtil.getString("chat.kicked"));
				} catch (ArrayIndexOutOfBoundsException e) {
					con.sendServerMessage(LangUtil.getString("chat.playerEmpty"));
				}
			}
			return true;
		}
	}

	public static class TeamChatCallback implements ChatCommandListener {
		@Override
		public boolean onSend(RoomConnection con, String[] args) {
			// TODO: Implement this method
			for (RoomConnection conn : con.currectRoom.connectionManager.getConnections()) {
				if (args.length < 1) return false;
				if (conn.player.team == con.player.team && conn != null) {
					conn.sendMessage(con.player.name,
									 LangUtil.getString("chat.teamMsg") + args[0],
									 con.player.playerIndex);
				}
			}
			return false;
		}
	}

	public static class MapsCallback implements ChatCommandListener {
		private int type;
		public MapsCallback(int type) {
			this.type = type;
		}
		@Override
		public boolean onSend(RoomConnection con, String[] args) {
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
				if (con.player.isAdmin && args.length > 0) {
					if (args[0].startsWith("'")) {
						String mapString = args[0].split("'")[1];
						for (int i=0;i < OfficialMap.mapsName.length;i++) {
							if (OfficialMap.mapsName[i].contains(mapString)) {
								Rukkit.getRoundConfig().mapName = OfficialMap.maps[i];
								Rukkit.getRoundConfig().mapType = 0;
								try {
									con.currectRoom.broadcast(Packet.serverInfo(con.currectRoom.config));
									con.handler.ctx.writeAndFlush(Packet.serverInfo(con.currectRoom.config, true));
								} catch (IOException ignored) {}
								break;
							}
						}
						//ChannelGroups.broadcast(new Packet().chat(p.playerName, "-map " + cmd[1], p.playerIndex));
						return false;
					}
					int id = Integer.parseInt(args[0]);
					Rukkit.getRoundConfig().mapName = OfficialMap.maps[id];
					Rukkit.getRoundConfig().mapType = 0;
				}
			}
			return false;
		}
	}

	public static class CustomMapsCallback implements ChatCommandListener {
		private final int type;
		public CustomMapsCallback(int type) {
			this.type = type;
		}
		@Override
		public boolean onSend(RoomConnection con, String[] args) {
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
				if (con.player.isAdmin && args.length > 0) {
					ArrayList<String> mapList = CustomMapLoader.getMapNameList();
					int id = Integer.parseInt(args[0]);
					Rukkit.getRoundConfig().mapName = mapList.get(id).toString();
					Rukkit.getRoundConfig().mapType = 1;
					try {
						con.currectRoom.broadcast(Packet.serverInfo(con.currectRoom.config));
						con.handler.ctx.writeAndFlush(Packet.serverInfo(con.currectRoom.config, true));
					} catch (IOException ignored) {}
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
		public boolean onSend(RoomConnection con, String[] cmd) {
			switch (type) {
					//move
				case 0:
					if (!con.player.isAdmin || con.currectRoom.isGaming() || cmd.length < 2) {
						// Do nothing.
					} else {
						PlayerManager playerGroup = con.currectRoom.playerManager;
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
					if (con.currectRoom.isGaming() || cmd.length < 1) {
						// Do nothing.
					} else {
						try {
							if (con.player.movePlayer(Integer.parseInt(cmd[0]) - 1)) {
								con.sendServerMessage(LangUtil.getString("chat.moveComplete"));
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

	// TODO: -qc 操作
	class QcCallback implements ChatCommandListener {
		@Override
		public boolean onSend(RoomConnection con, String[] args) {
			if (args.length <= 0) return false;
			getLogger().debug(args[0]);
			Rukkit.getCommandManager().executeChatCommand(con, args[0].substring(1));
			return false;
		}
	}

	class TeamCallback implements ChatCommandListener {
		private int type;
		public TeamCallback(int type) {
			this.type = type;
		}
		@Override
		public boolean onSend(RoomConnection con, String[] args) {
			switch (type) {
					//team
				case 0:
					if (con.currectRoom.isGaming() || !con.player.isAdmin || args.length < 2) {
						// Do nothing.
					} else {
						try {
							con.currectRoom.playerManager
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

			}
			return false;
		}
	}

	static class HelpCallback implements ChatCommandListener {
		@Override
		public boolean onSend(RoomConnection con, String[] args) {
			// TODO: Implement this method
			StringBuilder build = new StringBuilder();
			if (args.length > 0) {
				build.append("- Help -  Page " + args[0] + " \n");
				int page = Integer.valueOf(args[0]) - 1;
				for (int i = page * 10;i < Rukkit.getCommandManager().getLoadedCommand().entrySet().size();i++) {
					if (i > page * 10 + 10) break;
					ChatCommand cmd = (ChatCommand) ((Map.Entry) Rukkit.getCommandManager().getLoadedCommand().entrySet().toArray()[i]).getValue();
					build.append(String.format("%s : %s", cmd.cmd, cmd.helpMessage) + "\n");
				}
			} else {
				build.append("- Help -  Page 1 \n");
				for (int i = 0;i < Rukkit.getCommandManager().getLoadedCommand().entrySet().size();i++) {
					if (i > 10) break;
					ChatCommand cmd = (ChatCommand) ((Map.Entry) Rukkit.getCommandManager().getLoadedCommand().entrySet().toArray()[i]).getValue();
					build.append(String.format("%s : %s", cmd.cmd, cmd.helpMessage) + "\n");
				}
			}
			con.sendServerMessage(build.toString());
			return false;
		}
	}

	class InfoCallback implements ChatCommandListener {
		@Override
		public boolean onSend(RoomConnection con, String[] args) {
			log.warn("{} send a info: {}", con.player.name, args[0]);
			return false;
		}
	}

	class StartCallback implements ChatCommandListener {
		@Override
		public boolean onSend(RoomConnection con, String[] args) {
			if (con.currectRoom.isGaming() || !con.player.isAdmin) {
				// Do nothing.
			} else {
				if (con.currectRoom.connectionManager.size() < Rukkit.getConfig().minStartPlayer) {
					con.currectRoom.connectionManager.broadcastServerMessage(MessageFormat.format(LangUtil.getString("chat.minStartPlayer"), Rukkit.getConfig().minStartPlayer));
				} else {
					con.currectRoom.startGame();
				}
			}
			return true;
		}
	}

	class SetFogCallback implements ChatCommandListener {
		@Override
		public boolean onSend(RoomConnection con, String[] args) {
			if (con.currectRoom.isGaming() || !con.player.isAdmin || args.length < 1) {
				// Do nothing.
			} else {
				RoundConfig cfg = Rukkit.getRoundConfig();
				switch (args[0]) {
					case "off":
						cfg.fogType = 0;
						break;
					case "basic":
						cfg.fogType = 1;
						break;
					case "los":
						cfg.fogType = 2;
						break;
					default:
						cfg.fogType = 2;
				}
				try {
					con.currectRoom.broadcast(Packet.serverInfo(con.currectRoom.config));
					con.handler.ctx.writeAndFlush(Packet.serverInfo(con.currectRoom.config, true));
				} catch (IOException ignored) {}
			}
			return false;
		}
	}

	class StartingUnitCallback implements ChatCommandListener {
		@Override
		public boolean onSend(RoomConnection con, String[] args) {
			if (con.currectRoom.isGaming() || !con.player.isAdmin || args.length < 1) {
				// Do nothing.
			} else {
				Rukkit.getRoundConfig().startingUnits = Integer.parseInt(args[0]);
				try {
					con.currectRoom.broadcast(Packet.serverInfo(con.currectRoom.config));
					con.handler.ctx.writeAndFlush(Packet.serverInfo(con.currectRoom.config, true));
				} catch (IOException ignored) {}
			}
			return false;
		}
	}

	class ShareCallback implements ChatCommandListener {
		@Override
		public boolean onSend(RoomConnection con, String[] args) {
			if (con.currectRoom.isGaming() || args.length < 1) {
				// Do nothing.
			} else {
				RoomConnectionManager ChannelGroups = con.currectRoom.connectionManager;
				switch (args[0]) {
					case "on":
						con.player.isSharingControl = true;
						ChannelGroups.broadcastServerMessage(con.player.name + "stopped Shared control!");
						break;
					case "off":
						con.player.isSharingControl = false;
						ChannelGroups.broadcastServerMessage(con.player.name + "started Shared control.");
						break;
					default:
						con.player.isSharingControl = false;
						ChannelGroups.broadcastServerMessage(con.player.name + "started Shared control!");
				}
			}
			return false;
		}
	}

	class SharedControlCallback implements ChatCommandListener {
		@Override
		public boolean onSend(RoomConnection con, String[] args) {
			if (con.currectRoom.isGaming() || !con.player.isAdmin || args.length < 1) {
				// Do nothing.
			} else {
				Rukkit.getRoundConfig().sharedControl = Boolean.parseBoolean(args[0]);
				try {
					con.currectRoom.broadcast(Packet.serverInfo(con.currectRoom.config));
					con.handler.ctx.writeAndFlush(Packet.serverInfo(con.currectRoom.config, true));
				} catch (IOException ignored) {}
			}
			return false;
		}
	}

	class NukeCallback implements ChatCommandListener {
		@Override
		public boolean onSend(RoomConnection con, String[] args) {
			if (con.currectRoom.isGaming() || !con.player.isAdmin || args.length < 1) {
				// Do nothing.
			} else {
				Rukkit.getRoundConfig().disableNuke = !Boolean.parseBoolean(args[0]);
				try {
					con.currectRoom.broadcast(Packet.serverInfo(con.currectRoom.config));
					con.handler.ctx.writeAndFlush(Packet.serverInfo(con.currectRoom.config, true));
				} catch (IOException ignored) {}
			}
			return false;
		}
	}

	class IncomeCallback implements ChatCommandListener {
		@Override
		public boolean onSend(RoomConnection con, String[] args) {
			if (con.currectRoom.isGaming() || !con.player.isAdmin || args.length < 1) {
				// Do nothing.
			} else {
				Rukkit.getRoundConfig().income = Integer.parseInt(args[0]);
				if (Rukkit.getRoundConfig().income > 100 && Rukkit.getRoundConfig().income < 0) {
					Rukkit.getRoundConfig().income = 1;
				}
				try {
					con.currectRoom.broadcast(Packet.serverInfo(con.currectRoom.config));
					con.handler.ctx.writeAndFlush(Packet.serverInfo(con.currectRoom.config, true));
				} catch (IOException ignored) {}
			}
			return false;
		}
	}

	class CreditsCallback implements ChatCommandListener {
		@Override
		public boolean onSend(RoomConnection con, String[] args) {
			if (con.currectRoom.isGaming() || !con.player.isAdmin || args.length < 1) {
				// Do nothing.
			} else {
				Rukkit.getRoundConfig().credits = Integer.parseInt(args[0]);
				try {
					con.currectRoom.broadcast(Packet.serverInfo(con.currectRoom.config));
					con.handler.ctx.writeAndFlush(Packet.serverInfo(con.currectRoom.config, true));
				} catch (IOException ignored) {}
			}
			return false;
		}
	}

    class SyncCallback implements ChatCommandListener {
        @Override
        public boolean onSend(RoomConnection con, String[] args) {
            if (con.player.isAdmin) {
                con.currectRoom.syncGame();
            }
            return false;
        }
    }

    class DumpSyncCallBack implements ChatCommandListener {
        @Override
        public boolean onSend(RoomConnection con, String[] args) {

            return false;
        }
    }

	class ChksumCallback implements ChatCommandListener {
		@Override
		public boolean onSend(RoomConnection con, String[] args) {
			try {
				con.currectRoom.broadcast(Packet.syncCheckSum(con.currectRoom));
			} catch (IOException e) {
				//con.sendChat(
			}
			return false;
		}
	}

	class PingCallBack implements ChatCommandListener {
		@Override
		public boolean onSend(RoomConnection con, String[] args) {
			if (args.length >= 2) {
				float x = Float.parseFloat(args[0]);
				float y = Float.parseFloat(args[1]);
				//String name = args[0];
				try {
					con.currectRoom.broadcast(Packet.gamePing(con.currectRoom, con.player.playerIndex, PingType.happy, x, y));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return false;
		}
	}

	static class StateCallback implements ChatCommandListener {
		@Override
		public boolean onSend(RoomConnection con, String[] args) {
			// TODO: Implement this method
			StringBuilder build = new StringBuilder();
			build.append("- State - \n");
			build.append("RAM Usage: " +  (Runtime.getRuntime().freeMemory() / 10240) + "M/" + (Runtime.getRuntime().totalMemory()) / 10240 + "M\n");
			build.append("Connections: " + Rukkit.getGlobalConnectionManager().size());
			build.append("ThreadManager Tasks: " + Rukkit.getThreadManager().getActiveThreadCount() + "/" + Rukkit.getConfig().threadPoolCount);
			try {
				con.handler.ctx.writeAndFlush(Packet.chat("SERVER",
						build.toString(), -1));
			} catch (IOException e) {}
			return false;
		}
	}

	class PlayerListCallback implements ChatCommandListener {
		@Override
		public boolean onSend(RoomConnection con, String[] args) {
			StringBuffer buffer = new StringBuffer("- Players -\n");
			for (RoomConnection conn: con.currectRoom.connectionManager.getConnections()) {
				buffer.append(String.format("%s (Team %d) (%d ms)\n",conn.player.name, conn.player.team, (System.currentTimeMillis() - conn.pingTime)));
			}
			con.sendServerMessage(buffer.toString());
			return false;
		}
	}

	class SurrenderCallback implements ChatCommandListener {
		@Override
		public boolean onSend(RoomConnection con, String[] args) {
			if (!con.player.isSurrounded) {
				try {
					con.currectRoom.broadcast(Packet.gameSurrounder(con.currectRoom, con.player.playerIndex));
					con.currectRoom.connectionManager.broadcastServerMessage(String.format("Player %s surrounded!", con.player.name));
					con.player.isSurrounded = true;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return false;
		}
	}

	/*class InfoCallback implements ChatCommandListener {
	 @Override
	 public boolean onSend(Connection con, String[] args) {
	 return false;
	 }
	 }*/


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
		mgr.registerCommand(new ChatCommand("map", LangUtil.getString("chat.map"), 1, new MapsCallback(1), this, true));
		mgr.registerCommand(new ChatCommand("cmaps", LangUtil.getString("chat.cmaps"), 1, new CustomMapsCallback(0), this));
		mgr.registerCommand(new ChatCommand("cmap", LangUtil.getString("chat.cmap"), 1, new CustomMapsCallback(1), this, true));
		mgr.registerCommand(new ChatCommand("kick", LangUtil.getString("chat.kick"), 1, new KickCallBack(), this, true));
		mgr.registerCommand(new ChatCommand("team", LangUtil.getString("chat.team"), 2, new TeamCallback(0), this, true));
		mgr.registerCommand(new ChatCommand("self_team", LangUtil.getString("chat.self_team"), 1, new TeamCallback(1), this));
		mgr.registerCommand(new ChatCommand("move", LangUtil.getString("chat.move"), 2, new MoveCallback(0), this, true));
		mgr.registerCommand(new ChatCommand("self_move", LangUtil.getString("chat.self_move"), 2, new MoveCallback(1), this));
		mgr.registerCommand(new ChatCommand("qc", LangUtil.getString("chat.qc"), 1, new QcCallback(), this));
		mgr.registerCommand(new ChatCommand("fog", LangUtil.getString("chat.fog"), 1, new SetFogCallback(), this, true));
		mgr.registerCommand(new ChatCommand("nukes", LangUtil.getString("chat.nukes"), 1, new NukeCallback(), this, true));
		mgr.registerCommand(new ChatCommand("startingunits", LangUtil.getString("chat.startingunits"), 1, new StartingUnitCallback(), this, true));
		mgr.registerCommand(new ChatCommand("income", LangUtil.getString("chat.income"), 1, new IncomeCallback(), this, true));
		mgr.registerCommand(new ChatCommand("share", LangUtil.getString("chat.share"), 1, new ShareCallback(), this));
		mgr.registerCommand(new ChatCommand("credits", LangUtil.getString("chat.credits"), 1, new CreditsCallback(), this, true));
		mgr.registerCommand(new ChatCommand("start", LangUtil.getString("chat.start"), 1, new StartCallback(), this, true));
        mgr.registerCommand(new ChatCommand("sync", LangUtil.getString("chat.sync"), 0, new SyncCallback(), this, true));
		mgr.registerCommand(new ChatCommand("i", LangUtil.getString("chat.i"), 1, new InfoCallback(), this));
		mgr.registerCommand(new ChatCommand("chksum", LangUtil.getString("chat.chksum"), 0, new ChksumCallback(), this));
		mgr.registerCommand(new ChatCommand("maping", LangUtil.getString("chat.maping"), 2, new PingCallBack(), this));
		mgr.registerCommand(new ChatCommand("list", LangUtil.getString("chat.list"), 0, new PlayerListCallback(), this));
		mgr.registerCommand(new ChatCommand("surrender", LangUtil.getString("chat.surrender"), 0, new SurrenderCallback(), this));
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

}
