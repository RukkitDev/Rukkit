package cn.rukkit.network;
import io.netty.channel.*;
import io.netty.channel.group.*;
import io.netty.util.concurrent.*;
import java.util.*;
import cn.rukkit.game.*;

public class ConnectionManager
{
	List<Connection> connections = new ArrayList<Connection>();
	private static final ChannelGroup CHANNEL_GROUP = new DefaultChannelGroup("ChannelGroups", GlobalEventExecutor.INSTANCE);

	private PlayerManager playerManager;

	private GameServer server;

	public void add(Connection connection)
	{
		connections.add(connection);
		playerManager.add(connection.player);
		CHANNEL_GROUP.add(connection.handler.ctx.channel());
	}

	public ChannelGroupFuture broadcast(Object msg)
	{
		return CHANNEL_GROUP.writeAndFlush(msg);
	}

	public ChannelGroupFuture broadcast(Object msg, ChannelMatcher matcher)
	{
		return CHANNEL_GROUP.writeAndFlush(msg, matcher);
	}

	public ChannelGroup flush()
	{
		return CHANNEL_GROUP.flush();
	}

	public boolean discard(Connection connection)
	{
		connections.remove(connection);
		playerManager.remove(connection.player);
		return CHANNEL_GROUP.remove(connection.handler.ctx.channel());
	}

	public ChannelGroupFuture disconnect()
	{
		return CHANNEL_GROUP.disconnect();
	}

	public ChannelGroupFuture disconnect(ChannelMatcher matcher)
	{
		return CHANNEL_GROUP.disconnect(matcher);
	}

	public boolean contains(Channel channel)
	{
		return CHANNEL_GROUP.contains(channel);
	}

	public int size()
	{
		return CHANNEL_GROUP.size();
	}
	
	public ConnectionManager(GameServer server) {
		this.server = server;
	}
	
	public void getPlayerAsList() {
		
	}
	
	public PlayerManager getPlayerManager() {
		return playerManager;
	}
	
	public void registerPlayer(Connection connection) {
		
	}
}
