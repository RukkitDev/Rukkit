/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.command;

import java.util.*;

import cn.rukkit.util.LangUtil;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.completer.StringsCompleter;
import org.slf4j.*;
import cn.rukkit.network.*;
import cn.rukkit.network.room.ServerGlobalConnectionManager;
import cn.rukkit.network.room.ServerRoomConnection;
import cn.rukkit.*;
import cn.rukkit.network.packet.*;
import java.io.*;

public class CommandManager 
{
	private Logger log = LoggerFactory.getLogger(CommandManager.class);
	private HashMap<String, ChatCommand> loadedCommand = new HashMap<String, ChatCommand>();
	private HashMap<String, ServerCommand> loadedServerCommand = new HashMap<String, ServerCommand>();

	private List<String> serverCmdString = new ArrayList<>();
	
	public void registerCommand(ChatCommand cmd) {
		log.debug(String.format("Registering Command '%s' from plugin '%s'...",cmd.cmd,cmd.getFromPlugin().config.name));
		if (cmd.getContextListener() == null
				&& cmd.getListener() instanceof ChatCommandContextListener contextListener) {
			cmd.setContextListener(contextListener);
		}
		if (fetchCommand(cmd.cmd) != null) {
			log.warn(String.format("Command '%s' had already registered.",cmd.cmd));
		} else {
			loadedCommand.put(cmd.cmd, cmd);
		}
	}

	public void registerServerCommand(ServerCommand cmd) {
		log.debug(String.format("Registering ServerCommand '%s' from plugin '%s'...",cmd.cmd,cmd.getFromPlugin().config.name));
		if (fetchServerCommand(cmd.cmd) != null) {
			log.warn(String.format("ServerCommand '%s' had already registered.",cmd.cmd));
		} else {
			loadedServerCommand.put(cmd.cmd, cmd);
			serverCmdString.add(cmd.cmd);
		}
	}

	/**
	 * Normalizes the optional prefix accepted by the {@code qc} command.
	 *
	 * <p>The nested command may be written as {@code command},
	 * {@code -command}, or {@code .command}. Only one prefix is removed.</p>
	 */
	public static String normalizeNestedCommand(String command) {
		if (command == null || command.isEmpty()) {
			return command;
		}
		char prefix = command.charAt(0);
		return prefix == '-' || prefix == '.' ? command.substring(1) : command;
	}
	
	public void executeChatCommand(RoomConnection connection, String cmd) {
		String[] cmds = cmd.split("\\s+", 2);
		ChatCommand cmdObj = fetchCommand(cmds[0]);
		if (cmdObj == null) {
			connection.sendServerMessage(LangUtil.getString("chat.invalidCommand"));
			return;
		} else if (cmdObj.adminRequired){
			if (!connection.player.isAdmin) {
				connection.sendServerMessage(LangUtil.getString("chat.privDenied"));
				return;
			}
		}
		boolean result;
		log.trace("cmd is:" + cmds[0]);
		if (cmds.length > 1 && cmdObj.args > 0) {
			String[] args = cmds[1].split(" ", cmdObj.args);
			result = cmdObj.getListener().onSend(connection,args);
		} else {
			result = cmdObj.getListener().onSend(connection,new String[0]);
		}
		if (result == true) {
			try {
				connection.currectRoom.broadcast(
					Packet.chat(connection.player.name,
								"-" + cmd,
								connection.player.playerIndex));
			} catch (IOException e) {}
		}
	}

	/**
	 * Execute a command for the migrated connection stack.
	 *
	 * <p>The legacy {@link ChatCommandListener} is intentionally not invoked
	 * here because it requires a legacy {@link RoomConnection}. Commands are
	 * migrated by assigning a {@link ChatCommandContextListener} to the
	 * command; until then the command is reported as not migrated.</p>
	 */
	public void executeChatCommand(ServerRoomConnection connection, String cmd) {
		executeChatCommand(connection, null, cmd);
	}

	/** Execute a command with migrated global runtime services. */
	public void executeChatCommand(ServerRoomConnection connection,
	                              ServerGlobalConnectionManager globalConnectionManager,
	                              String cmd) {
		ChatCommandContext context = new ServerChatCommandContext(connection, globalConnectionManager);
		String[] cmds = cmd.split("\\s+", 2);
		ChatCommand cmdObj = fetchCommand(cmds[0]);
		if (cmdObj == null) {
			context.sendServerMessage(LangUtil.getString("chat.invalidCommand"));
			return;
		}
		if (cmdObj.adminRequired && !context.player().isAdmin) {
			context.sendServerMessage(LangUtil.getString("chat.privDenied"));
			return;
		}

		ChatCommandContextListener listener = cmdObj.getContextListener();
		if (listener == null) {
			log.warn("Command '{}' has no migrated context listener", cmds[0]);
			return;
		}

		log.trace("cmd is:{}", cmds[0]);
		String[] args = cmds.length > 1 && cmdObj.args > 0
				? cmds[1].split(" ", cmdObj.args)
				: new String[0];
		if (listener.onSend(context, args)) {
			context.broadcastCommandEcho(cmd);
		}
	}

	public void executeServerCommand(String cmd) {
		String[] cmds = cmd.split("\\s+", 2);
		ServerCommand cmdObj = fetchServerCommand(cmds[0]);
		if (cmdObj == null) {
			log.info("Command '{}' not found. Try 'help' to list all commands.", cmds[0]);
			return;
		}
		log.trace("cmd is:" + cmds[0]);
		if (cmds.length > 1 && cmdObj.args > 0) {
			String[] args = cmds[1].split(" ", cmdObj.args);
			cmdObj.getListener().onSend(args);
		} else {
			cmdObj.getListener().onSend(new String[0]);
		}
	}

	
	public ChatCommand fetchCommand(String cmd){
		return loadedCommand.getOrDefault(cmd, null);
	}

	public ServerCommand fetchServerCommand(String cmd) {
		return loadedServerCommand.getOrDefault(cmd, null);
	}
	
	public HashMap<String, ChatCommand> getLoadedCommand() {
		return loadedCommand;
	}

	public HashMap<String, ServerCommand> getLoadedServerCommand() {
		return loadedServerCommand;
	}

	public List<String> getLoadedServerCommandStringList() {
		return serverCmdString;
	}
}
