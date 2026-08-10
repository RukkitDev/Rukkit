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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Global connection registry for the migrated room runtime. */
public class ServerGlobalConnectionManager {
    private final List<ServerRoomConnection> connections = new CopyOnWriteArrayList<>();
    private final ChannelGroup channelGroup;
    private final ServerRoomManager roomManager;
    private final Logger log = LoggerFactory.getLogger(ServerGlobalConnectionManager.class);

    public ServerGlobalConnectionManager(ServerRoomManager roomManager) {
        this.roomManager = roomManager;
        this.channelGroup = new DefaultChannelGroup("ServerChannelGroups", GlobalEventExecutor.INSTANCE);
    }

    public synchronized void add(ServerRoomConnection connection) {
        if (connection == null || connection.handler == null || connection.handler.ctx == null
                || connections.contains(connection)) {
            return;
        }
        for (ServerRoomConnection existing : connections) {
            if (existing != connection && existing.player == connection.player) {
                connections.remove(existing);
                if (existing.handler != null && existing.handler.ctx != null) {
                    channelGroup.remove(existing.handler.ctx.channel());
                }
            }
        }
        connections.add(connection);
        channelGroup.add(connection.handler.ctx.channel());
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
        connections.remove(connection);
        if (connection.handler != null && connection.handler.ctx != null) {
            connection.handler.ctx.disconnect();
            return channelGroup.remove(connection.handler.ctx.channel());
        }
        return false;
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

    public NetworkPlayer getPlayerByName(String name) {
        for (ServerRoomConnection connection : connections) {
            if (connection.player != null && connection.player.name != null
                    && connection.player.name.equals(name)) {
                return connection.player;
            }
        }
        return null;
    }

    public NetworkPlayer getPlayerByUUID(String uuid) {
        for (ServerRoomConnection connection : connections) {
            if (connection.player != null && connection.player.uuid != null
                    && connection.player.uuid.equals(uuid)) {
                return connection.player;
            }
        }
        return null;
    }

    public NetworkPlayer getAllPlayerByUUID(String uuid) {
        if (uuid == null) {
            return null;
        }
        synchronized (roomManager) {
            for (ServerRoom room : new ArrayList<>(roomManager.roomList)) {
                NetworkPlayer player = room.playerManager.getPlayerByUUID(uuid);
                if (player != null && !player.isEmpty) {
                    return player;
                }
            }
        }
        return null;
    }

    public void broadcastGlobalServerMessage(String message) {
        try {
            broadcast(UniversalPacket.chat("SERVER", message, -1));
        } catch (IOException ignored) {
            log.debug("Unable to create global server message packet", ignored);
        }
    }
}
