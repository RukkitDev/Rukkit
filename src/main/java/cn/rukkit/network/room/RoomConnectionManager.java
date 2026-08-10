/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.room;

import cn.rukkit.game.NetworkPlayer;
import cn.rukkit.game.PlayerManager;
import cn.rukkit.game.SaveData;
import cn.rukkit.network.ConnectionState;
import cn.rukkit.network.core.packet.Packet;
import cn.rukkit.network.core.packet.UniversalPacket;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.ChannelMatcher;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Master-compatible connection manager for {@link ServerRoom}. */
public class RoomConnectionManager {
    private final ServerRoom room;
    public final List<ServerRoomConnection> connections = new CopyOnWriteArrayList<>();
    private final ChannelGroup channelGroup;
    private final PlayerManager playerManager;
    private final Logger log;

    public RoomConnectionManager(ServerRoom room) {
        this.room = room;
        this.playerManager = room.playerManager;
        this.log = LoggerFactory.getLogger("RoomConnectionManager #" + room.roomId);
        this.channelGroup = new DefaultChannelGroup(
                "ChannelGroups" + room.roomId, GlobalEventExecutor.INSTANCE);
    }

    /**
     * Retains the master API surface while the player-list representation is
     * still owned by {@link PlayerManager}.
     */
    public void getPlayerAsList() {
        // Kept for source compatibility with master callers.
    }

    public synchronized boolean add(ServerRoomConnection connection) {
        if (connection == null || connection.player == null || connection.handler == null
                || connection.handler.ctx == null) {
            return false;
        }
        if (connections.contains(connection)) {
            return true;
        }
        if (!playerManager.addWithTeam(connection.player)) {
            return false;
        }
        connections.add(connection);
        channelGroup.add(connection.handler.ctx.channel());
        return true;
    }

    public synchronized boolean set(ServerRoomConnection connection, int index) {
        if (connection == null || connection.player == null || connection.handler == null
                || connection.handler.ctx == null) {
            return false;
        }
        NetworkPlayer currentPlayer = playerManager.get(index);
        if (currentPlayer == null || (!currentPlayer.isEmpty && currentPlayer != connection.player)) {
            return false;
        }
        connection.player.playerIndex = index;
        playerManager.set(index, connection.player);
        for (ServerRoomConnection existing : connections) {
            if (existing != connection && existing.player == connection.player) {
                connections.remove(existing);
                existing.stopPingTask();
                existing.stopTeamTask();
                if (existing.handler != null && existing.handler.ctx != null) {
                    channelGroup.remove(existing.handler.ctx.channel());
                }
            }
        }
        if (connections.contains(connection)) {
            return true;
        }
        connections.add(connection);
        channelGroup.add(connection.handler.ctx.channel());
        return true;
    }

    public ChannelGroupFuture broadcast(Packet packet) {
        return channelGroup.writeAndFlush(packet);
    }

    public ChannelGroupFuture broadcast(Packet packet, ChannelMatcher matcher) {
        return channelGroup.writeAndFlush(packet, matcher);
    }

    public ChannelGroup flush() {
        return channelGroup.flush();
    }

    public synchronized boolean discard(ServerRoomConnection connection) {
        if (connection == null) {
            return false;
        }
        boolean wasRegistered = connections.remove(connection);
        boolean currentConnection = connection.player != null
                && connection.player.getServerConnection() == connection;
        if (wasRegistered && currentConnection) {
            boolean wasAdmin = connection.player.isAdmin;
            playerManager.remove(connection.player);
            if (wasAdmin) {
                connection.player.isAdmin = false;
                transferAdminToLiveConnection();
            }
        }
        if (connection.handler != null && connection.handler.ctx != null) {
            connection.handler.ctx.disconnect();
            return channelGroup.remove(connection.handler.ctx.channel());
        }
        return false;
    }

    private void transferAdminToLiveConnection() {
        for (ServerRoomConnection candidate : connections) {
            if (!isLive(candidate)) {
                continue;
            }
            candidate.player.isAdmin = true;
            try {
                candidate.sendPacket(UniversalPacket.serverInfo(room.config, true));
            } catch (IOException ignored) {
            }
            return;
        }
    }

    private boolean isLive(ServerRoomConnection connection) {
        return connection != null
                && connection.player != null
                && !connection.player.isEmpty
                && !connection.player.isDisconnected
                && connection.player.getServerConnection() == connection
                && connection.handler != null
                && connection.handler.getState() != ConnectionState.DISCONNECTED
                && connection.handler.ctx != null
                && connection.handler.ctx.channel().isOpen();
    }

    public ChannelGroupFuture disconnect() {
        return channelGroup.disconnect();
    }

    public ChannelGroupFuture disconnect(ChannelMatcher matcher) {
        return channelGroup.disconnect(matcher);
    }

    public boolean contains(ServerRoomConnection connection) {
        return connections.contains(connection);
    }

    public int size() {
        return connections.size();
    }

    public List<ServerRoomConnection> getConnections() {
        return connections;
    }

    public SaveData getAvailableSave() {
        for (ServerRoomConnection connection : connections) {
            if (connection.save != null) {
                log.debug("Get client save, tick={}, server tick={}",
                        connection.save.time, room.getCurrentStep());
                if (Math.abs(connection.save.time - room.getCurrentStep()) < Integer.MAX_VALUE) {
                    return connection.save;
                }
            }
        }
        return null;
    }

    public void clearAllSaveData() {
        for (ServerRoomConnection connection : connections) {
            connection.save = null;
        }
    }

    public void broadcastServerMessage(String message) {
        try {
            broadcast(UniversalPacket.chat("SERVER", message, -1));
        } catch (IOException ignored) {
        }
    }

    public void broadcastGlobalServerMessage(String message) {
        broadcastServerMessage(message);
    }

    public void broadcastServerInfo() {
        try {
            broadcast(UniversalPacket.serverInfo(room.config, false));
        } catch (IOException ignored) {
        }
    }
}
