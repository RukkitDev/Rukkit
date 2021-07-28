package cn.rukkit.command;

import cn.rukkit.game.mod.*;
import java.util.*;
import org.slf4j.*;
import cn.rukkit.network.*;

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
		log.debug("cmd is:" + cmds[0]);
		if (cmds.length > 1 && cmdobj.args > 0) {
			String[] args = cmds[1].split(" ", cmdobj.args - 1);
			cmdobj.getListener().onSend(connection,args);
		} else {
			cmdobj.getListener().onSend(connection,null);
		}
	}
	
	public ChatCommand fetchCommand(String cmd){
		return loadedCommand.getOrDefault(cmd, null);
	}
	
	public HashMap<String, ChatCommand> getLoadedCommand() {
		return loadedCommand;
	}
}
