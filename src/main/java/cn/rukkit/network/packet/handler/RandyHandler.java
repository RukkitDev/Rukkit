/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.packet.handler;

import cn.rukkit.network.ConnectionState;
import cn.rukkit.network.packet.Packet;

import java.util.List;

public class RandyHandler extends PacketHandler{
    @Override
    public int getType() {
        return Packet.PACKET_RANDY;
    }

    @Override
    public List<ConnectionState> getAllowedStates() {
        return List.of(ConnectionState.IN_ROOM, ConnectionState.IN_GAME);
    }

    @Override
    public void handle(PacketContext ctx, Packet packet) throws Exception {
        ctx.connection().currectRoom.connectionManager.broadcastServerMessage(String.format("Player '%s' is randy.", ctx.connection().player.name));
    }
}
