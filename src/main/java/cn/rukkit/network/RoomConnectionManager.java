/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network;

import cn.rukkit.Rukkit;
import cn.rukkit.game.NetworkPlayer;
import cn.rukkit.game.PlayerManager;
import cn.rukkit.game.SaveData;
import cn.rukkit.network.packet.Packet;
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

public class RoomConnectionManager {
    private final NetworkRoom room;
    volatile List<RoomConnection> connections = new ArrayList<RoomConnection>();
    private ChannelGroup CHANNEL_GROUP;

    private PlayerManager playerManager;

    private RoomGameServer server;

    private Logger log;

    /**
     * Add a connection to list.
     * When player registered,this function will add it into management.
     */
    public void add(RoomConnection connection) {
        connections.add(connection);
        playerManager.addWithTeam(connection.player);
        CHANNEL_GROUP.add(connection.handler.ctx.channel());
    }

    public void set(RoomConnection connection, int index) {
        connections.add(connection);
        playerManager.set(index, connection.player);
        CHANNEL_GROUP.add(connection.handler.ctx.channel());
    }

    /**
     * Broadcast a message to all connections.
     *
     * @params msg Packet msg.
     */
    public ChannelGroupFuture broadcast(Packet msg) {
        return CHANNEL_GROUP.writeAndFlush(msg);
    }

    /**
     * Broadcast a message to all connections,but using a ChannelMatcher.
     *
     * @params msg Packet msg.
     * @params matcher ChannelMatcher.
     */
    public ChannelGroupFuture broadcast(Packet msg, ChannelMatcher matcher) {
        return CHANNEL_GROUP.writeAndFlush(msg, matcher);
    }

    /**
     * Flush all connections.
     */
    public ChannelGroup flush() {
        return CHANNEL_GROUP.flush();
    }

    /**
     * Discard a connection.Auto disconnect when connection discorded.
     *
     * @params connection Connection to discard.
     */
    public boolean discard(RoomConnection connection) {
        connection.handler.ctx.disconnect();
        connections.remove(connection);
        playerManager.remove(connection.player);
        // Check privs.
        if (connection.player.isAdmin && playerManager.getPlayerCount() > 0) {
            for (NetworkPlayer p : playerManager.getPlayerArray()) {
                if (!p.isEmpty) {
                    p.isAdmin = true;
                    try {
                        p.getConnection().handler.ctx.writeAndFlush(Packet.serverInfo(room.config, true));
                    } catch (IOException ignored) {}
                    break;
                }
            }
        }
        return CHANNEL_GROUP.remove(connection.handler.ctx.channel());
    }

    /**
     * Disconnect all Connections.
     */
    public ChannelGroupFuture disconnect() {
        return CHANNEL_GROUP.disconnect();
    }

    /**
     * Disconnect Connections with ChannelMatcher.
     *
     * @params matcher ChannelMatcher
     */
    public ChannelGroupFuture disconnect(ChannelMatcher matcher) {
        return CHANNEL_GROUP.disconnect(matcher);
    }

    /**
     * check a connection whether in Group.
     *
     * @params connection need to be checked.
     */
    public boolean contains(RoomConnection connection) {
        return CHANNEL_GROUP.contains(connection.handler.ctx.channel());
    }

    /**
     * Return Group size.
     */
    public int size() {
        return CHANNEL_GROUP.size();
    }

    /**
     * Init a ConnectionManager.
     *
     * @params server GameServer
     */
    public RoomConnectionManager(NetworkRoom room) {
        this.room = room;
        playerManager = room.playerManager;
        log = LoggerFactory.getLogger("RoomConnectionManager #" + room.roomId);
        CHANNEL_GROUP = new DefaultChannelGroup("ChannelGroups" + room.roomId, GlobalEventExecutor.INSTANCE);
    }

    public void getPlayerAsList() {

    }

    /**
     * Get connections
     */
    public List<RoomConnection> getConnections() {
        return connections;
    }

//    public void registerPlayer(Connection connection) {
//
//    }

    public SaveData getAvailableSave() {
        for (RoomConnection conn : connections) {
            if (conn.save != null) {
                log.debug("Get client save, tick={}, server tick={}", conn.save.time, room.getCurrentStep());
                if (Math.abs(conn.save.time - room.getCurrentStep()) < Integer.MAX_VALUE) {
                    return conn.save;
                }
            }
        }
        return null;
    }

    public void clearAllSaveData() {
        for (RoomConnection conn : connections) {
            conn.save = null;
        }
    }

    public void broadcastServerMessage(String msg) {
        try {
            broadcast(Packet.chat("SERVER", msg, -1));
        } catch (IOException ignored) {
        }
    }

    public void broadcastGlobalServerMessage(String msg) {
        try {
            broadcast(Packet.chat("SERVER", msg, -1));
        } catch (IOException ignored) {
        }
    }

    /**
     * Broadcast server info.(No admin trigger.)
     */
    public void broadcastServerInfo() {
        try {
            broadcast(Packet.serverInfo(room.config, false));
        } catch (IOException ignored) {
        }
    }

}
