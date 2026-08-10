/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.command;

import cn.rukkit.network.room.ServerRoom;
import cn.rukkit.network.room.ServerRoomConnection;

/**
 * Optional room capability for commands that need the current core room.
 *
 * <p>Keeping this capability separate from {@link ChatCommandContext} prevents
 * room-specific state from becoming part of every command's base contract.</p>
 */
public interface RoomCommandContext extends ChatCommandContext {
    ServerRoom room();

    ServerRoomConnection connection();
}
