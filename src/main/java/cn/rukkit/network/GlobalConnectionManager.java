/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network;
import io.netty.channel.group.*;
import io.netty.util.concurrent.*;

import java.io.IOException;
import java.util.*;
import cn.rukkit.game.*;
import cn.rukkit.network.packet.*;
import cn.rukkit.*;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class GlobalConnectionManager
{
	List<RoomConnection> connections = new ArrayList<RoomConnection>();
	private static final ChannelGroup CHANNEL_GROUP = new DefaultChannelGroup("ChannelGroups", GlobalEventExecutor.INSTANCE);

	//private PlayerManager playerManager;

	private RoomGameServer server;
    
    private Logger log = LoggerFactory.getLogger(GlobalConnectionManager.class);

	/**
	* Add a connection to list.
	* When player registered,this function will add it into management.
	*/
	public void add(RoomConnection connection)
	{
		connections.add(connection);
		//playerManager.addWithTeam(connection.player);
		CHANNEL_GROUP.add(connection.handler.ctx.channel());
	}

//	@Deprecated
//	public void set(RoomConnection connection, int index) {
//		connections.add(connection);
//		//playerManager.set(index, connection.player);
//		CHANNEL_GROUP.add(connection.handler.ctx.channel());
//	}
	
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
	public boolean discard(RoomConnection connection)
	{
		connection.handler.ctx.disconnect();
		connections.remove(connection);
		//playerManager.remove(connection.player);
		// Check privs.
//		if (connection.player.isAdmin && Rukkit.getConnectionManager().getPlayerManager().getPlayerCount() > 0) {
//			for (NetworkPlayer p : playerManager.getPlayerArray()) {
//				if (!p.isEmpty) {
//					p.isAdmin = true;
//					break;
//				}
//			}
//		}
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
	public boolean contains(RoomConnection connection)
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
	public GlobalConnectionManager(RoomGameServer server) {
		this.server = server;
		//playerManager = new PlayerManager(Rukkit.getConfig().maxPlayer);
	}

	/**
	 * 根据名称获取玩家对象
	 * @param name
	 * @return
	 */
	public NetworkPlayer getPlayerByName(String name) {
		for (RoomConnection conn: connections) {
			if (conn.player.name.equals(name)) {
				return conn.player;
			}
		}
		return null;
	}

	/**
	 * 根据UUID获取玩家对象（推荐使用，更加可靠）
	 * @param UUID
	 * @return
	 */
	public NetworkPlayer getPlayerByUUID(String UUID) {
		for (RoomConnection conn: connections) {
			if (conn.player.uuid.equals(UUID)) {
				return conn.player;
			}
		}
		return null;
	}

	/**
	 * 根据UUID获取玩家对象（包括可断线重连的玩家）
	 * 效率较低谨慎使用！
	 * @param UUID
	 * @return
	 */
	public NetworkPlayer getAllPlayerByUUID(String UUID) {
		for (NetworkRoom room: Rukkit.getRoomManager().roomList) {
			NetworkPlayer player;
			if ((player = room.playerManager.getPlayerByUUID(UUID)) != null) {
				return player;
			}
		}
		return null;
	}
	
	/**
	* Get playerManager.
	*/
//	public PlayerManager getGlobalPlayerManager() {
//		return playerManager;
//	}
	
	/**
	* Get connections
	*/
	public List<RoomConnection> getConnections() {
		return connections;
	}

//	@Deprecated
//	public void registerPlayer(Connection connection) {
//
//	}

//	@Deprecated
//    public SaveData getAvailableSave() {
//        for (Connection conn : connections) {
//            if (conn.save != null) {
//                log.debug("Get client save, tick={}, server tick={}", conn.save.time, Rukkit.getGameServer().getTickTime());
//                if (Math.abs(conn.save.time - server.getTickTime()) < Integer.MAX_VALUE) {
//                    return conn.save;
//                }
//            }
//        }
//        return null;
//    }
//
//	@Deprecated
//    public void clearAllSaveData() {
//        for (Connection conn : connections) {
//            conn.save = null;
//        }
//    }

//	@Deprecated
//	public void broadcastServerMessage(String msg) {
//		try {
//			broadcast(Packet.chat("SERVER", msg, -1));
//		} catch (IOException ignored) {}
//	}

	public void broadcastGlobalServerMessage(String msg) {
		try {
			broadcast(Packet.chat("SERVER", msg, -1));
		} catch (IOException ignored) {}
	}

	/**
	 * Broadcast server info.(No admin trigger.)
	 */
//	@Deprecated
//	public void broadcastServerInfo() {
//		try {
//			broadcast(Packet.serverInfo(false));
//		} catch (IOException ignored) {}
//	}

}
