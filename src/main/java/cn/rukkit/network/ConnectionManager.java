/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network;
import io.netty.channel.*;
import io.netty.channel.group.*;
import io.netty.util.concurrent.*;

import java.io.IOException;
import java.util.*;
import cn.rukkit.game.*;
import cn.rukkit.network.packet.*;
import cn.rukkit.*;
import cn.rukkit.util.MathUtil;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class ConnectionManager
{
	List<Connection> connections = new ArrayList<Connection>();
	private static final ChannelGroup CHANNEL_GROUP = new DefaultChannelGroup("ChannelGroups", GlobalEventExecutor.INSTANCE);

	private PlayerManager playerManager;

	private GameServer server;
    
    private Logger log = LoggerFactory.getLogger(ConnectionManager.class);

	/**
	* Add a connection to list.
	* When player registered,this function will add it into management.
	*/
	public void add(Connection connection)
	{
		connections.add(connection);
		playerManager.addWithTeam(connection.player);
		CHANNEL_GROUP.add(connection.handler.ctx.channel());
	}

	public void set(Connection connection, int index) {
		connections.add(connection);
		playerManager.set(index, connection.player);
		CHANNEL_GROUP.add(connection.handler.ctx.channel());
	}
	
	/**
	* Broadcast a message to all connections.
	* @params msg Packet msg.
	*/
	public ChannelGroupFuture broadcast(Packet msg)
	{
		return CHANNEL_GROUP.writeAndFlush(msg);
	}

	/**
	 * Broadcast a message to all connections,but using a ChannelMatcher.
	 * @params msg Packet msg.
	 * @params matcher ChannelMatcher.
	 */
	public ChannelGroupFuture broadcast(Packet msg, ChannelMatcher matcher)
	{
		return CHANNEL_GROUP.writeAndFlush(msg, matcher);
	}
	
	/**
	 * Flush all connections.
	 */
	public ChannelGroup flush()
	{
		return CHANNEL_GROUP.flush();
	}
	
	/**
	* Discard a connection.Auto disconnect when connection discorded.
	* @params connection Connection to discard.
	*/
	public boolean discard(Connection connection)
	{
		connection.handler.ctx.disconnect();
		connections.remove(connection);
		playerManager.remove(connection.player);
		// Check privs.
		if (connection.player.isAdmin && Rukkit.getConnectionManager().getPlayerManager().getPlayerCount() > 0) {
			for (NetworkPlayer p : playerManager.getPlayerArray()) {
				if (!p.isEmpty) {
					p.isAdmin = true;
					break;
				}
			}
		}
		return CHANNEL_GROUP.remove(connection.handler.ctx.channel());
	}

	/**
	* Disconnect all Connections.
	*/
	public ChannelGroupFuture disconnect()
	{
		return CHANNEL_GROUP.disconnect();
	}
	
	/**
	* Disconnect Connections with ChannelMatcher.
	* @params matcher ChannelMatcher
	*/
	public ChannelGroupFuture disconnect(ChannelMatcher matcher)
	{
		return CHANNEL_GROUP.disconnect(matcher);
	}
	
	/**
	* check a connection whether in Group.
	* @params connection Connection need to be checked.
	*/
	public boolean contains(Connection connection)
	{
		return CHANNEL_GROUP.contains(connection.handler.ctx.channel());
	}
	
	/**
	* Return Group size.
	*/
	public int size()
	{
		return CHANNEL_GROUP.size();
	}
	
	/**
	* Init a ConnectionManager.
	* @params server GameServer
	*/
	public ConnectionManager(GameServer server) {
		this.server = server;
		playerManager = new PlayerManager(Rukkit.getConfig().maxPlayer);
	}
	
	public void getPlayerAsList() {
		
	}
	
	/**
	* Get playerManager.
	*/
	public PlayerManager getPlayerManager() {
		return playerManager;
	}
	
	/**
	* Get connections
	*/
	public List<Connection> getConnections() {
		return connections;
	}
	
	public void registerPlayer(Connection connection) {

	}
    
    public SaveData getAvailableSave() {
        for (Connection conn : connections) {
            if (conn.save != null) {
                log.debug("Get client save, tick={}, server tick={}", conn.save.time, Rukkit.getGameServer().getTickTime());
                if (Math.abs(conn.save.time - server.getTickTime()) < Integer.MAX_VALUE) {
                    return conn.save;
                }
            }
        }
        return null;
    }
    
    public void clearAllSaveData() {
        for (Connection conn : connections) {
            conn.save = null;
        }
    }

	public void broadcastServerMessage(String msg) {
		try {
			broadcast(Packet.chat("SERVER", msg, -1));
		} catch (IOException ignored) {}
	}

	/**
	 * Broadcast server info.(No admin trigger.)
	 */
	public void broadcastServerInfo() {
		try {
			broadcast(Packet.serverInfo(false));
		} catch (IOException ignored) {}
	}

}
