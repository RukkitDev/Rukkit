/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.command;

import cn.rukkit.Rukkit;
import cn.rukkit.network.core.packet.UniversalPacket;
import cn.rukkit.network.room.ServerGlobalConnectionManager;
import cn.rukkit.network.room.ServerRoomConnection;

import java.io.IOException;
import java.util.Objects;

/** Context adapter for commands executed by the migrated network stack. */
public final class ServerChatCommandContext implements RoomCommandContext {
    private final ServerRoomConnection connection;
    private final ServerGlobalConnectionManager globalConnectionManager;

    public ServerChatCommandContext(ServerRoomConnection connection) {
        this(connection, null);
    }

    public ServerChatCommandContext(ServerRoomConnection connection,
                                    ServerGlobalConnectionManager globalConnectionManager) {
        this.connection = Objects.requireNonNull(connection, "connection must not be null");
        this.globalConnectionManager = globalConnectionManager;
    }

    @Override
    public cn.rukkit.game.NetworkPlayer player() {
        return connection.player;
    }

    @Override
    public cn.rukkit.network.room.ServerRoom room() {
        return connection.currectRoom;
    }

    @Override
    public ServerRoomConnection connection() {
        return connection;
    }

    @Override
    public int connectionCount() {
        if (globalConnectionManager != null) {
            return globalConnectionManager.size();
        }
        return connection.currectRoom.connectionManager.size();
    }

    @Override
    public int activeThreadCount() {
        return Rukkit.getThreadManager() == null
                ? 0 : Rukkit.getThreadManager().getActiveThreadCount();
    }

    @Override
    public int threadPoolCount() {
        return Rukkit.getConfig() == null ? 0 : Rukkit.getConfig().threadPoolCount;
    }

    @Override
    public void sendServerMessage(String message) {
        connection.sendServerMessage(message);
    }

    @Override
    public void broadcastCommandEcho(String command) {
        try {
            connection.currectRoom.connectionManager.broadcast(
                    UniversalPacket.chat(connection.player.name,
                            "-" + command,
                            connection.player.playerIndex));
        } catch (IOException ignored) {
        }
    }
}
