/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.command;
import cn.rukkit.plugin.*;

public class ChatCommand
{
	public String cmd;
	public String helpMessage;
	public int args = 1;
	public boolean adminRequired = false;
	private boolean isEnabled = false;
	private ChatCommandListener chatListener;
	private ChatCommandContextListener contextListener;
	private RukkitPlugin fromPlugin;


	public ChatCommand(String msg, String helpMessage,int args, ChatCommandListener chatListener, RukkitPlugin fromPlugin) {
		this.cmd = msg;
		this.args = args;
		this.helpMessage = helpMessage;
		this.chatListener = chatListener;
		this.fromPlugin = fromPlugin;
	}

	public ChatCommand(String msg, String helpMessage,int args, ChatCommandListener chatListener, RukkitPlugin fromPlugin, boolean adminRequired) {
		this.cmd = msg;
		this.args = args;
		this.helpMessage = helpMessage;
		this.chatListener = chatListener;
		this.fromPlugin = fromPlugin;
		this.adminRequired = adminRequired;
	}

	/**
	 * Creates a command owned by the migrated network stack.
	 *
	 * <p>This is a factory instead of an overloaded constructor so existing
	 * lambda calls targeting {@link ChatCommandListener} remain source
	 * compatible.</p>
	 */
	public static ChatCommand contextCommand(String msg, String helpMessage, int args,
			ChatCommandContextListener contextListener, RukkitPlugin fromPlugin) {
		return contextCommand(msg, helpMessage, args, contextListener, fromPlugin, false);
	}

	/** Creates a migrated command with an explicit administrator requirement. */
	public static ChatCommand contextCommand(String msg, String helpMessage, int args,
			ChatCommandContextListener contextListener, RukkitPlugin fromPlugin,
			boolean adminRequired) {
		ChatCommand command = new ChatCommand(msg, helpMessage, args,
				(ChatCommandListener) null, fromPlugin, adminRequired);
		command.setContextListener(contextListener);
		return command;
	}

	public RukkitPlugin getFromPlugin() {
		return fromPlugin;
	}

	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	public boolean isEnabled() {
		return isEnabled;
	}

	public void setListener(ChatCommandListener listener) {
		this.chatListener = listener;
	}

	public ChatCommandListener getListener() {
		return chatListener;
	}

	public void setContextListener(ChatCommandContextListener listener) {
		this.contextListener = listener;
	}

	public ChatCommandContextListener getContextListener() {
		return contextListener;
	}
}
