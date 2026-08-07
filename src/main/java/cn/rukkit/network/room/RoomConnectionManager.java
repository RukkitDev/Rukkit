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

/** Master-compatible connection manager for {@link ServerRoom}. */
public class RoomConnectionManager {
    private final ServerRoom room;
    public volatile List<ServerRoomConnection> connections = new ArrayList<>();
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

    public void add(ServerRoomConnection connection) {
        connections.add(connection);
        playerManager.addWithTeam(connection.player);
        channelGroup.add(connection.handler.ctx.channel());
    }

    public void set(ServerRoomConnection connection, int index) {
        connections.add(connection);
        playerManager.set(index, connection.player);
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

    public boolean discard(ServerRoomConnection connection) {
        connection.handler.ctx.disconnect();
        connections.remove(connection);
        playerManager.remove(connection.player);
        if (connection.player.isAdmin && playerManager.getPlayerCount() > 0) {
            for (NetworkPlayer player : playerManager.getPlayerArray()) {
                if (!player.isEmpty && player.getServerConnection() != null) {
                    player.isAdmin = true;
                    try {
                        player.getServerConnection().sendPacket(
                                UniversalPacket.serverInfo(room.config, true));
                    } catch (IOException ignored) {
                    }
                    break;
                }
            }
        }
        return channelGroup.remove(connection.handler.ctx.channel());
    }

    public ChannelGroupFuture disconnect() {
        return channelGroup.disconnect();
    }

    public ChannelGroupFuture disconnect(ChannelMatcher matcher) {
        return channelGroup.disconnect(matcher);
    }

    public boolean contains(ServerRoomConnection connection) {
        return channelGroup.contains(connection.handler.ctx.channel());
    }

    public int size() {
        return channelGroup.size();
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
