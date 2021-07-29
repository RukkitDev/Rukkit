package cn.rukkit.command;

import cn.rukkit.game.mod.*;
import java.util.*;
import org.slf4j.*;
import cn.rukkit.network.*;
import cn.rukkit.*;
import cn.rukkit.network.packet.*;
import java.io.*;

public class CommandManager 
{
	private Logger log = LoggerFactory.getLogger(CommandManager.class);
	private HashMap<String, ChatCommand> loadedCommand = new HashMap<String, ChatCommand>();
	
	public void registerCommand(ChatCommand cmd) {
		log.debug(String.format("Registering Command '%s' from plugin '%s'...",cmd.cmd,cmd.getFromPlugin().config.name));
		if (fetchCommand(cmd.cmd) != null) {
			log.warn(String.format("Command '%s' had already registered.",cmd.cmd));
		} else {
			loadedCommand.put(cmd.cmd, cmd);
		}
	}
	
	public void execute(Connection connection, String cmd) {
		String cmds[] = cmd.split(" ", 1);
		ChatCommand cmdobj = fetchCommand(cmd);
		boolean result;
		log.debug("cmd is:" + cmds[0]);
		if (cmds.length > 1 && cmdobj.args > 0) {
			String[] args = cmds[1].split(" ", cmdobj.args - 1);
			result = cmdobj.getListener().onSend(connection,args);
		} else {
			result = cmdobj.getListener().onSend(connection,new String[0]);
		}
		if (result == true) {
			try {
				Rukkit.getConnectionManager().broadcast(
					Packet.chat(connection.player.name,
								"-" + cmd,
								connection.player.playerIndex));
			} catch (IOException e) {}
		}
	}
	
	public ChatCommand fetchCommand(String cmd){
		return loadedCommand.getOrDefault(cmd, null);
	}
	
	public HashMap<String, ChatCommand> getLoadedCommand() {
		return loadedCommand;
	}
}
