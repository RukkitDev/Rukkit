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

public class Command
{
    public enum CommandType {
        CHAT, SERVER, BOTH
    }
    public String cmd;
    public String helpMessage;
    public int args = 1;
    private boolean isEnabled = false;
    private ChatCommandListener chatListener;
    private RukkitPlugin fromPlugin;
    public CommandType type;

    public Command(String msg, String helpMessage,int args, ChatCommandListener chatListener, RukkitPlugin fromPlugin, CommandType type) {
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
