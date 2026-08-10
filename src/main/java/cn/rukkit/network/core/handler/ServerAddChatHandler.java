/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.core.handler;

import cn.rukkit.event.player.PlayerChatEvent;
import cn.rukkit.network.ConnectionState;
import cn.rukkit.network.core.packet.Packet;
import cn.rukkit.network.core.packet.PacketType;
import cn.rukkit.network.core.packet.UniversalPacket;
import cn.rukkit.network.io.GameInputStream;
import cn.rukkit.network.room.ServerRoomConnection;

import java.util.List;
import java.util.Objects;

/**
 * Master-compatible ordinary chat handler for the core packet stack.
 *
 * <p>Chat command dispatch is intentionally kept as a separate migration
 * step because the legacy command API accepts {@code RoomConnection}, while
 * this handler owns a {@code ServerRoomConnection}.</p>
 */
public class ServerAddChatHandler extends ServerPacketHandler {
    private final ServerChatCommandDispatcher commandDispatcher;

    /** Creates a handler with command dispatch left for the compatibility layer. */
    public ServerAddChatHandler() {
        this.commandDispatcher = null;
    }

    public ServerAddChatHandler(ServerChatCommandDispatcher commandDispatcher) {
        this.commandDispatcher = Objects.requireNonNull(
                commandDispatcher, "commandDispatcher must not be null");
    }

    @Override
    public int getType() {
        return PacketType.ADD_CHAT;
    }

    @Override
    public List<ConnectionState> getAllowedStates() {
        return List.of(ConnectionState.IN_ROOM, ConnectionState.IN_GAME);
    }

    @Override
    public void handle(ServerPacketContext context, Packet packet) throws Exception {
        ServerRoomConnection connection = context.connection();
        if (connection == null || connection.player == null) {
            return;
        }

        GameInputStream input = new GameInputStream(packet);
        String chatMessage = input.readString();
        if (chatMessage.startsWith(".")
                || chatMessage.startsWith("-")
                || chatMessage.startsWith("_")) {
            if (commandDispatcher == null) {
                getLogger().debug("Chat command dispatch is not migrated yet: {}", chatMessage);
            } else {
                commandDispatcher.dispatch(connection, chatMessage.substring(1));
            }
            return;
        }

        if (PlayerChatEvent.getListenerList().callListeners(
                new PlayerChatEvent(connection.player, chatMessage))) {
            connection.currectRoom.connectionManager.broadcast(
                    UniversalPacket.chat(connection.player.name,
                            chatMessage,
                            connection.player.playerIndex));
        }
    }
}
