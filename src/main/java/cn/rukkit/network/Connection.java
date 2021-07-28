package cn.rukkit.network;
import cn.rukkit.game.*;
import io.netty.channel.*;
import cn.rukkit.command.*;

public class Connection {
	public NetworkPlayer player;
	public ConnectionHandler handler;
	//public ChannelHandlerContext ctx;

	/**
	* Ping runnable.
	*/
	public class PingTasker implements Runnable {
		@Override
		public void run() {
			// TODO: Implement this method
		}
	}

	/**
	* TeamTask Scheduler.
	*/
	public class TeamTasker implements Runnable {
		@Override
		public void run() {
			// TODO: Implement this method
		}
	}

	public Connection(ConnectionHandler handler) {
		this.handler = handler;
	}
}
