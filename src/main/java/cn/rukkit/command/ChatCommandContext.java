/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.command;

import cn.rukkit.game.NetworkPlayer;

/**
 * Application-level capabilities exposed to a chat command.
 *
 * <p>The context deliberately hides the concrete network connection and
 * packet implementation so the same command semantics can serve both
 * network stacks during migration.</p>
 */
public interface ChatCommandContext {
    NetworkPlayer player();

    int connectionCount();

    int activeThreadCount();

    int threadPoolCount();

    void sendServerMessage(String message);

    void broadcastCommandEcho(String command);
}
