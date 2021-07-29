package cn.rukkit.plugin.internal;
//import cn.rukkit.plugin.InternalRukkitPlugin;
import cn.rukkit.*;
import cn.rukkit.command.*;
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

	@Override
	public void onLoad() {
		// TODO: Implement this method
		getLogger().info("CommandPlugin::onLoad()");
		ChatCommand cmd = new ChatCommand("help", "Show help.", 1,
			new ChatCommandListener() {

				@Override
				public boolean onSend(Connection con, String[] args) {
					// TODO: Implement this method
					StringBuilder build = new StringBuilder();
					if (args.length > 1) {
						build.append("- Help -  Page " + args[0] + " \n");
						int page = Integer.valueOf(args[0]);
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
			}, this);

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
