/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.command;

import cn.rukkit.plugin.RukkitPlugin;

public class ServerCommand {
    public String cmd;
    public String helpMessage;
    public int args = 1;
    private boolean isEnabled = false;
    private ServerCommandListener serverCommandListener;
    private RukkitPlugin fromPlugin;

    public ServerCommand(String msg, String helpMessage,int args, ServerCommandListener chatListener, RukkitPlugin fromPlugin) {
        this.cmd = msg;
        this.args = args;
        this.helpMessage = helpMessage;
        this.serverCommandListener = chatListener;
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

    public void setListener(ServerCommandListener listener) {
        this.serverCommandListener = listener;
    }

    public ServerCommandListener getListener() {
        return serverCommandListener;
    }
}
