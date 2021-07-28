package cn.rukkit.network;
import io.netty.channel.*;
import io.netty.channel.group.*;
import io.netty.util.concurrent.*;
import java.util.*;
import cn.rukkit.game.*;
import cn.rukkit.network.packet.*;

public class ConnectionManager
{
	List<Connection> connections = new ArrayList<Connection>();
	private static final ChannelGroup CHANNEL_GROUP = new DefaultChannelGroup("ChannelGroups", GlobalEventExecutor.INSTANCE);

	private PlayerManager playerManager;

	private GameServer server;

	/**
	* Add a connection to list.
	* When player registered,this function will add it into management.
	*/
	public void add(Connection connection)
	{
		connections.add(connection);
		playerManager.add(connection.player);
		CHANNEL_GROUP.add(connection.handler.ctx.channel());
	}
	
	/**
	* Broadcast a message to all connections.
	* @params mag Packet msg.
	*/
	public ChannelGroupFuture broadcast(Packet msg)
	{
		return CHANNEL_GROUP.writeAndFlush(msg);
	}

	/**
	 * Broadcast a message to all connections,but using a ChannelMatcher.
	 * @params mag Packet msg.
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
	}
	
	public void getPlayerAsList() {
		
	}
	
	/**
	* Get playerManager.
	*/
	public PlayerManager getPlayerManager() {
		return playerManager;
	}
	
	public void registerPlayer(Connection connection) {
		
	}
}
