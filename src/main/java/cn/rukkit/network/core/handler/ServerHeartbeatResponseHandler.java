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

import java.util.List;

/** Master-compatible heartbeat response handler for the core packet stack. */
public class ServerHeartbeatResponseHandler extends ServerPacketHandler {
    @Override
    public int getType() {
        return PacketType.HEART_BEAT_RESPONSE;
    }

    @Override
    public List<ConnectionState> getAllowedStates() {
        return List.of(ConnectionState.IN_ROOM, ConnectionState.IN_GAME);
    }

    @Override
    public void handle(ServerPacketContext context, Packet packet) {
        if (context.connection() != null) {
            context.connection().pong();
        }
    }
}
