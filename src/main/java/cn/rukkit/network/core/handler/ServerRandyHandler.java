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
import cn.rukkit.network.room.ServerRoomConnection;

import java.util.List;

/** Master-compatible READY/Randy notification handler for the core stack. */
public class ServerRandyHandler extends ServerPacketHandler {
    @Override
    public int getType() {
        return PacketType.READY;
    }

    @Override
    public List<ConnectionState> getAllowedStates() {
        return List.of(ConnectionState.IN_ROOM, ConnectionState.IN_GAME);
    }

    @Override
    public void handle(ServerPacketContext context, Packet packet) {
        ServerRoomConnection connection = context.connection();
        if (connection == null || connection.player == null || connection.currectRoom == null) {
            return;
        }
        connection.currectRoom.connectionManager.broadcastServerMessage(
                String.format("Player '%s' is randy.", connection.player.name));
    }
}
