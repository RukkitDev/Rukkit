package cn.rukkit.network;
import io.netty.channel.*;
import org.slf4j.*;
import cn.rukkit.network.packet.*;
import cn.rukkit.game.*;
import cn.rukkit.*;
import cn.rukkit.config.*;
import java.util.concurrent.*;

public class ConnectionHandler extends ChannelInboundHandlerAdapter
{
	Logger log = LoggerFactory.getLogger(ConnectionHandler.class);

	public ChannelHandlerContext ctx;
	private Connection conn;
	private ScheduledFuture timeoutFuture;
	public class TimeoutTask implements Runnable {
		private int execTime = 0;
		@Override
		public void run() {
			// TODO: Implement this method
			execTime ++;
			if (execTime >= Rukkit.getConfig().registerTimeout) {
				ctx.disconnect();
			}
		}
	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception
	{
		// TODO: Implement this method
		super.channelRegistered(ctx);
		// 保存 ctx 实例
		this.ctx = ctx;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception
	{
		// TODO: Implement this method
		super.channelActive(ctx);
		startTimeout();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception
	{
		// TODO: Implement this method
		super.channelInactive(ctx);
		Rukkit.getConnectionManager().discard(conn);
		conn.stopPingTask();
		conn.startTeamTask();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
	{
		// TODO: Implement this method
		super.channelRead(ctx, msg);
		Packet p = (Packet) msg;
		GameInputStream in = new GameInputStream(p);
		switch (p.type) {
			case Packet.PACKET_PREREGISTER_CONNECTION:
				log.info(String.format("New connection established:%s", ctx.channel().remoteAddress()));
				ctx.write(p.preRegister());
				ctx.writeAndFlush(p.chat("SERVER", "You are logging to Rukkit.", -1));
				break;
			case Packet.PACKET_PLAYER_INFO:
				ctx.writeAndFlush(p.serverInfo());
				String packageName = in.readString();
				log.debug("Ints:"+in.readInt());
				int gameVersionCode = in.readInt();
				in.readInt();
				String playerName = in.readString();
				in.readByte();
				in.readString();
				String uuid = in.readString();
				in.readInt();
				String verifyResult = in.readString();
				log.info(String.format("Got Player(package=%s, version=%d, name=%s, uuid=%s, verify=%s",
						packageName,gameVersionCode,playerName,uuid,verifyResult));
				//Check avaliable
				if (Rukkit.getConnectionManager().size() > Rukkit.getConfig().maxPlayer) {
					ctx.writeAndFlush(p.kick("Game is full!"));
					return;
				}
				//Init connection.
				conn = new Connection(this);
				NetworkPlayer player = new NetworkPlayer(conn);
				player.name = playerName;
				player.uuid = uuid;
				conn.player = player;
				//Adding into ConnectionManager.
				Rukkit.getConnectionManager().add(conn);
				conn.startPingTask();
				conn.startTeamTask();
				stopTimeout();
				break;
			case Packet.PACKET_HEART_BEAT_RESPONSE:
				conn.pong();
				break;
			case Packet.PACKET_ADD_CHAT:
				String chatmsg = in.readString();
				if (chatmsg.startsWith(".") || chatmsg.startsWith("-") || chatmsg.startsWith("_")) {
					Rukkit.getCommandManager().execute(conn ,chatmsg.substring(1));
				} else {
					Rukkit.getConnectionManager().broadcast(p.chat(conn.player.name, chatmsg, conn.player.playerIndex)); 
				}
				break;
			}
	}
	
	public void startTimeout() {
		if (timeoutFuture == null) {
			timeoutFuture = Rukkit.getThreadManager().schedule(new TimeoutTask(), 1000, 1000);
		}
	}
	
	public void stopTimeout() {
		if (timeoutFuture != null) {
			Rukkit.getThreadManager().shutdownTask(timeoutFuture);
			timeoutFuture = null;
		}
	}
}
