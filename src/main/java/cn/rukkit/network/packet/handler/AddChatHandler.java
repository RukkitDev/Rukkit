/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.packet.handler;

import cn.rukkit.Rukkit;
import cn.rukkit.event.player.PlayerChatEvent;
import cn.rukkit.network.ConnectionState;
import cn.rukkit.network.GameInputStream;
import cn.rukkit.network.RoomConnection;
import cn.rukkit.network.packet.Packet;

import java.util.List;

public class AddChatHandler extends PacketHandler{
    @Override
    public int getType() {
        return Packet.PACKET_ADD_CHAT;
    }

    @Override
    public List<ConnectionState> getAllowedStates() {
        return List.of(ConnectionState.IN_ROOM, ConnectionState.IN_GAME);
    }

    @Override
    public void handle(PacketContext ctx, Packet packet) throws Exception {
        RoomConnection connection = ctx.connection();
        if (connection == null || connection.player == null) {
            return;
        }
        GameInputStream stream = new GameInputStream(packet);
        String chatmsg = stream.readString();
        if (chatmsg.startsWith(".") || chatmsg.startsWith("-") || chatmsg.startsWith("_")) {
            Rukkit.getCommandManager().executeChatCommand(connection, chatmsg.substring(1));
        } else {
            if (PlayerChatEvent.getListenerList().callListeners(new PlayerChatEvent(connection.player, chatmsg))) {
                connection.currectRoom.connectionManager.broadcast(Packet.chat(connection.player.name, chatmsg, connection.player.playerIndex));
            }
        }
    }
}
