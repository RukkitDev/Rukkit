/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.core.handler;

import cn.rukkit.network.ConnectionState;
import cn.rukkit.network.core.packet.Packet;
import cn.rukkit.network.core.packet.PacketType;
import cn.rukkit.network.io.GameInputStream;

import java.util.List;

/** Master-compatible client disconnect handler for the core packet stack. */
public class ServerDisconnectHandler extends ServerPacketHandler {
    @Override
    public int getType() {
        return PacketType.DISCONNECT;
    }

    @Override
    public List<ConnectionState> getAllowedStates() {
        return List.of(ConnectionState.PRE_REGISTERED,
                ConnectionState.IN_ROOM,
                ConnectionState.IN_GAME);
    }

    @Override
    public void handle(ServerPacketContext context, Packet packet) throws Exception {
        GameInputStream input = new GameInputStream(packet);
        context.handler().setDisconnectReason(input.readString());
        context.transitionTo(ConnectionState.DISCONNECTED);
        context.ctx().disconnect();
    }
}
