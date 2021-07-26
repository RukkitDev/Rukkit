package cn.rukkit.network;
import cn.rukkit.game.*;
import io.netty.channel.*;
import cn.rukkit.command.*;

public class Connection
{
	public NetworkPlayer player;
	public ConnectionHandler handler;
	//public ChannelHandlerContext ctx;
	// Ping runnable.
	public class PingTasker {
		
	}
	// TeamTask Scheduler.
	public class TeamTasker {
		
	}
	public Connection(ConnectionHandler handler) {
		this.handler = handler;
	}
}
