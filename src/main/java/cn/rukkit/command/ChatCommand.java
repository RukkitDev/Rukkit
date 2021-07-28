package cn.rukkit.command;
import cn.rukkit.plugin.*;

public class ChatCommand
{
	public String cmd;
	public String helpMessage;
	public int args = 1;
	private boolean isEnabled = false;
	private ChatCommandListener chatListener;
	private RukkitPlugin fromPlugin;

	public ChatCommand(String msg, String helpMessage,int args, ChatCommandListener chatListener, RukkitPlugin fromPlugin) {
		this.cmd = msg;
		this.args = args;
		this.helpMessage = helpMessage;
		this.chatListener = chatListener;
		this.fromPlugin = fromPlugin;
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
}
