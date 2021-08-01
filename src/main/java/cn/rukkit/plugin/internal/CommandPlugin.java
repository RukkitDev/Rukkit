package cn.rukkit.plugin.internal;
//import cn.rukkit.plugin.InternalRukkitPlugin;
import cn.rukkit.*;
import cn.rukkit.command.*;
import cn.rukkit.config.RoundConfig;
import cn.rukkit.game.map.*;
import cn.rukkit.network.*;
import cn.rukkit.network.packet.*;
import cn.rukkit.plugin.*;
import java.io.*;
import cn.rukkit.game.*;
import java.util.*;

public class CommandPlugin extends InternalRukkitPlugin implements ChatCommandListener {

	@Override
	public boolean onSend(Connection con, String[] args) {
		// TODO: Implement this method
		StringBuilder build = new StringBuilder();
		build.append("Rukkit Server v" + Rukkit.RUKKIT_VERSION + "\n");
		build.append("Rukkit Plugin API v" + Rukkit.PLUGIN_API_VERSION);
		con.sendServerMessage(build.toString());
		return false;
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
		public boolean onSend(Connection con, String[] args) {
			// TODO: Implement this method
			if (con.player.isAdmin && args.length > 1 && !Rukkit.getGameServer().isGaming()) {
				int id = Integer.parseInt(args[1]);
				NetworkPlayer player = Rukkit.getConnectionManager().getPlayerManager().get(id);
				try {
					player.isNull();
					player.getConnection().kick("Kicked by admin.");
				} catch (ArrayIndexOutOfBoundsException e) {
					con.sendServerMessage("Player isn't exist!");
				}
			}
			return true;
		}
	}
	
	public class TeamChatCallback implements ChatCommandListener {
		@Override
		public boolean onSend(Connection con, String[] args) {
			// TODO: Implement this method
			for (Connection conn : Rukkit.getConnectionManager().getConnections()) {
				if (args.length < 1) return false;
				if (conn.player.team == con.player.team) {
					conn.sendMessage(con.player.name,
									args[1],
									con.player.playerIndex);
				}
			}
			return false;
		}
	}
	
	public class MapsCallback implements ChatCommandListener {
		private int type;
		public MapsCallback(int type) {
			this.type = type;
		}
		@Override
		public boolean onSend(Connection con, String[] args) {
			// TODO: Implement this method
			// Maps
			if (type == 0) {
				StringBuffer buf = new StringBuffer("- Maps -");
				for(int i=OfficialMap.maps.length - 1;i>=0;i--){
					buf.append(String.format("[%d] %s", i, OfficialMap.maps[i]) + "\n");
				}
				con.sendServerMessage(buf.toString());
			} else {
				if (con.player.isAdmin && args.length > 1) {
					int id = Integer.parseInt(args[1]);
					Rukkit.getRoundConfig().mapName = OfficialMap.maps[id];
					Rukkit.getRoundConfig().mapType = 0;
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
					if (!con.player.isAdmin && Rukkit.getGameServer().isGaming() && cmd.length < 2) {
						// Do nothing.
					} else {
						PlayerManager playerGroup = Rukkit.getConnectionManager().getPlayerManager();
						NetworkPlayer fromPlayer = playerGroup.get(Integer.parseInt(cmd[0]) - 1);
						NetworkPlayer targetPlayer = playerGroup.get(Integer.parseInt(cmd[1]) - 1);
						try{
							if(fromPlayer.movePlayer(Integer.parseInt(cmd[1]) - 1)){
								con.sendServerMessage("移动成功！");
							}else{
								int fromslot, toslot;
								fromslot = fromPlayer.playerIndex;
								toslot = targetPlayer.playerIndex;
								playerGroup.remove(targetPlayer);
								fromPlayer.movePlayer(toslot);
								targetPlayer.movePlayer(fromslot);
							}
						}catch(Exception e){
							//fromPlayer.movePlayer(Integer.parseInt(cmd[1]) - 1);
							e.printStackTrace();
						}
					}
					break;
				// Self-move
				case 1:
					if (Rukkit.getGameServer().isGaming() && cmd.length < 1) {
						// Do nothing.
					} else {
						try{
							if(con.player.movePlayer(Integer.parseInt(cmd[0]) - 1)){
								con.sendServerMessage("Move complete!");
							}else{
								con.sendServerMessage("Fail: already have a player in that slot");
							}
						}catch(Exception e){
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
		public boolean onSend(Connection con, String[] args) {
			if (args.length <= 0) return false;
			getLogger().debug(args[0]);
			Rukkit.getCommandManager().execute(con, args[0].substring(1));
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
					if (Rukkit.getGameServer().isGaming() && !con.player.isAdmin && args.length < 2) {
						// Do nothing.
					} else {
						try {
							Rukkit.getConnectionManager().getPlayerManager()
									.get(Integer.parseInt(args[0]) - 1).team = (Integer.parseInt(args[1]) - 1);
						} catch (NullPointerException e) {
							con.sendServerMessage("Player isn't exists!");
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

	class HelpCallback implements ChatCommandListener {
		@Override
		public boolean onSend(Connection con, String[] args) {
			// TODO: Implement this method
			StringBuilder build = new StringBuilder();
			if (args.length > 0) {
				build.append("- Help -  Page " + args[0] + " \n");
				int page = Integer.valueOf(args[0]) + 1;
				for (int i = page*10;i < Rukkit.getCommandManager().getLoadedCommand().entrySet().size();i++) {
					if (i > page*10+10) break;
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
		public boolean onSend(Connection con, String[] args) {
			con.sendChat("[info]" + args[0]);
			return false;
		}
	}

	class StartCallback implements ChatCommandListener {
		@Override
		public boolean onSend(Connection con, String[] args) {
			return false;
		}
	}

	class SetFogCallback implements ChatCommandListener {
		@Override
		public boolean onSend(Connection con, String[] args) {
			if (Rukkit.getGameServer().isGaming() && !con.player.isAdmin && args.length < 1) {
				// Do nothing.
			} else {
				RoundConfig cfg = Rukkit.getRoundConfig();
				switch(args[0]){
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
					Rukkit.getConnectionManager().broadcast(Packet.serverInfo());
					con.handler.ctx.writeAndFlush(Packet.serverInfo(true));
				} catch (IOException ignored) {}
			}
			return false;
		}
	}

	class StartingUnitCallback implements ChatCommandListener {
		@Override
		public boolean onSend(Connection con, String[] args) {
			if (Rukkit.getGameServer().isGaming() && !con.player.isAdmin && args.length < 1) {
				// Do nothing.
			} else {
				Rukkit.getRoundConfig().startingUnits = Integer.parseInt(args[0]);
				try {
					Rukkit.getConnectionManager().broadcast(Packet.serverInfo());
					con.handler.ctx.writeAndFlush(Packet.serverInfo(true));
				} catch (IOException ignored) {}
			}
			return false;
		}
	}

	class ShareCallback implements ChatCommandListener {
		@Override
		public boolean onSend(Connection con, String[] args) {
			if (Rukkit.getGameServer().isGaming() && args.length < 1) {
				// Do nothing.
			} else {
				ConnectionManager ChannelGroups = Rukkit.getConnectionManager();
				switch(args[0]){
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
		public boolean onSend(Connection con, String[] args) {
			if (Rukkit.getGameServer().isGaming() && !con.player.isAdmin && args.length < 1) {
				// Do nothing.
			} else {
				Rukkit.getRoundConfig().sharedControl = Boolean.parseBoolean(args[0]);
				try {
					Rukkit.getConnectionManager().broadcast(Packet.serverInfo());
					con.handler.ctx.writeAndFlush(Packet.serverInfo(true));
				} catch (IOException ignored) {}
			}
			return false;
		}
	}

	class NukeCallback implements ChatCommandListener {
		@Override
		public boolean onSend(Connection con, String[] args) {
			if (Rukkit.getGameServer().isGaming() && !con.player.isAdmin && args.length < 1) {
				// Do nothing.
			} else {
				Rukkit.getRoundConfig().disableNuke = !Boolean.parseBoolean(args[0]);
				try {
					Rukkit.getConnectionManager().broadcast(Packet.serverInfo());
					con.handler.ctx.writeAndFlush(Packet.serverInfo(true));
				} catch (IOException ignored) {}
			}
			return false;
		}
	}

	class IncomeCallback implements ChatCommandListener {
		@Override
		public boolean onSend(Connection con, String[] args) {
			if (Rukkit.getGameServer().isGaming() && !con.player.isAdmin && args.length < 1) {
				// Do nothing.
			} else {
				Rukkit.getRoundConfig().income = Integer.parseInt(args[0]);
				if (Rukkit.getRoundConfig().income > 100 && Rukkit.getRoundConfig().income < 0) {
					Rukkit.getRoundConfig().income = 1;
				}
				try {
					Rukkit.getConnectionManager().broadcast(Packet.serverInfo());
					con.handler.ctx.writeAndFlush(Packet.serverInfo(true));
				} catch (IOException ignored) {}
			}
			return false;
		}
	}

	class CreditsCallback implements ChatCommandListener {
		@Override
		public boolean onSend(Connection con, String[] args) {
			if (Rukkit.getGameServer().isGaming() && !con.player.isAdmin && args.length < 1) {
				// Do nothing.
			} else {
				Rukkit.getRoundConfig().credits = Integer.parseInt(args[0]);
				try {
					Rukkit.getConnectionManager().broadcast(Packet.serverInfo());
					con.handler.ctx.writeAndFlush(Packet.serverInfo(true));
				} catch (IOException ignored) {}
			}
			return false;
		}
	}


	@Override
	public void onLoad() {
		// TODO: Implement this method
		getLogger().info("CommandPlugin::onLoad()");
		ChatCommand cmd = new ChatCommand("help", "Show help.", 1, new HelpCallback(), this);
		ChatCommand state = new ChatCommand("state", "Show Server State.", 0,
			new ChatCommandListener() {

				@Override
				public boolean onSend(Connection con, String[] args) {
					// TODO: Implement this method
					StringBuilder build = new StringBuilder();
					build.append("- State - \n");
					build.append("RAM Usage: " +  (Runtime.getRuntime().freeMemory() / 10240) + "M/" + (Runtime.getRuntime().totalMemory()) / 10240 + "M\n");
					build.append("Connections: " + Rukkit.getConnectionManager().size());
					build.append("ThreadManager Tasks: " + Rukkit.getThreadManager().getActiveThreadCount() + "/" + Rukkit.getConfig().threadPoolCount);
					try {
						con.handler.ctx.writeAndFlush(Packet.chat("SERVER",
																  build.toString(), -1));
					} catch (IOException e) {}
					return false;
				}
			}, this);
		CommandManager mgr = Rukkit.getCommandManager();
		mgr.registerCommand(cmd);
		mgr.registerCommand(state);
		mgr.registerCommand(new ChatCommand("version", "Show Rukkit Version.", 0, this, this));
		mgr.registerCommand(new ChatCommand("team", "Send a team message.", 1, new TeamChatCallback(), this));
		mgr.registerCommand(new ChatCommand("t", "Send a team message.", 1, new TeamChatCallback(), this));
		mgr.registerCommand(new ChatCommand("maps", "Get official maps list.", 0, new MapsCallback(0), this));
		mgr.registerCommand(new ChatCommand("map", "Change map to map with id in map list.", 1, new MapsCallback(1), this));
		mgr.registerCommand(new ChatCommand("kick", "Kick a player.", 1, new KickCallBack(), this));
		mgr.registerCommand(new ChatCommand("team", "Change a player's ally.", 2, new TeamCallback(0), this));
		mgr.registerCommand(new ChatCommand("self_team", "Change yourself ally.", 1, new TeamCallback(1), this));
		mgr.registerCommand(new ChatCommand("move", "Move a player.", 2, new MoveCallback(0), this));
		mgr.registerCommand(new ChatCommand("self_move", "Move yourself.", 2, new MoveCallback(1), this));
		mgr.registerCommand(new ChatCommand("qc", "Execute a command silently.", 1, new QcCallback(), this));
		mgr.registerCommand(new ChatCommand("fog", "Set fog type in game.", 1, new SetFogCallback(), this));
		mgr.registerCommand(new ChatCommand("nukes", "Set nukes enabled in game.", 1, new NukeCallback(), this));
		mgr.registerCommand(new ChatCommand("startingunits", "Set starting units in game.", 1, new StartingUnitCallback(), this));
		mgr.registerCommand(new ChatCommand("income", "Set income in game(1x-100x).", 1, new IncomeCallback(), this));
		mgr.registerCommand(new ChatCommand("share", "Set your share state in game.(on/off)", 1, new ShareCallback(), this));
		mgr.registerCommand(new ChatCommand("credits", "Set default credits in game.", 1, new CreditsCallback(), this));
		mgr.registerCommand(new ChatCommand("start", "Start a game.", 1, new StartCallback(), this));
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
