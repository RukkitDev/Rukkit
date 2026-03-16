/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network;

public enum ConnectionState {
    CONNECTED, // 加入时的默认状态
    PRE_REGISTERED, // 预注册完成
    IN_ROOM, // 已进入房间
    IN_GAME, // 已进入游戏中
    DISCONNECTED // 断开连接
}
