/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.packet.handler;

import cn.rukkit.network.ConnectionHandler;
import cn.rukkit.network.ConnectionState;
import cn.rukkit.network.RoomConnection;
import io.netty.channel.ChannelHandlerContext;

/**
 * Packet 上下文
 * @param ctx   ChannelHandlerContext
 * @param connection 房间连接
 * @param handler 连接处理器
 * @since 0.9.2
 */
public record PacketContext (
        ChannelHandlerContext ctx,
        RoomConnection connection,
        ConnectionHandler handler
) {
    /**
     * 获取当前状态
     * @return ConnectionState 当前状态
     * @since 0.9.2
     */
    public ConnectionState state() {
        return handler.getState();
    }

    /**
     * 状态转换
     * @param state 新状态
     * @since 0.9.2
     */
    public void transitionTo(ConnectionState state) {
        handler.setState(state);
    }

    /**
     * 绑定房间连接
     * @param connection 房间连接
     * @since 0.9.2
     **/
    public void bindConnection(RoomConnection connection) {
        handler.setConn(connection);
    }
}
