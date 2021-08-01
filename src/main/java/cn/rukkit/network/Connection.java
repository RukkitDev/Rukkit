package cn.rukkit.network;
import cn.rukkit.game.*;
import cn.rukkit.network.packet.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import cn.rukkit.*;
import cn.rukkit.util.*;

public class Connection {
	public NetworkPlayer player;
	public ConnectionHandler handler;
	public long pingTime;
	private ScheduledFuture pingFuture;
	private ScheduledFuture teamFuture;
	//public ChannelHandlerContext ctx;

	/**
	 * Ping runnable.
	 */
	public class PingTasker implements Runnable {
		@Override
		public void run() {
			// TODO: Implement this method
			try {
				GameOutputStream o = new GameOutputStream();
				o.writeLong(new Random().nextLong());
				o.writeByte(0);
				Packet p = o.createPacket(108);
				handler.ctx.writeAndFlush(p);
				pingTime = System.currentTimeMillis();
			} catch (IOException e) {
				stopPingTask();
			}
		}
	}

	/**
	 * TeamTask Scheduler.
	 */
	public class TeamTasker implements Runnable {
		@Override
		public void run() {
			// TODO: Implement this method
			GameOutputStream o = new GameOutputStream();
			try
			{
				//log.d("Sending teamlist...");
				o.writeInt(player.playerIndex);
				// 1.14新增
				o.writeBoolean(Rukkit.getGameServer().isGaming());
				o.writeInt(Rukkit.getConfig().maxPlayer); //maxPlayer
				//1.14启用Gzip压缩
				GzipEncoder enc = o.getEncodeStream("teams", true);

				for (int i =0;i < Rukkit.getConfig().maxPlayer;i++)
				{
					NetworkPlayer playerp = Rukkit.getConnectionManager().getPlayerManager().get(i);
					enc.stream.writeBoolean(playerp != null);
					if (playerp == null) continue;
					//1.14
					//enc.stream.writeByte(0);
					enc.stream.writeInt(255);
					playerp.writePlayer(enc.stream);
				}
				o.flushEncodeData(enc);

				o.writeInt(Rukkit.getRoundConfig().fogType);
				o.writeInt(GameUtils.getMoneyFormat(Rukkit.getRoundConfig().credits));
				o.writeBoolean(true);
				//ai
				o.writeInt(1);
				//
				o.writeByte(4);
				//maxUnit
				o.writeInt(250);
				o.writeInt(250);

				//初始单位
				o.writeInt(Rukkit.getRoundConfig().startingUnits);
				o.writeFloat(Rukkit.getRoundConfig().income);
				o.writeBoolean(Rukkit.getRoundConfig().disableNuke);
				o.writeBoolean(false);
				o.writeBoolean(false);
				o.writeBoolean(Rukkit.getRoundConfig().sharedControl);

				Packet p = o.createPacket(Packet.PACKET_TEAM_LIST);

				handler.ctx.writeAndFlush(p);
			}
			catch (IOException e)
			{
				stopTeamTask();
				//log.e(e);
				//e.printStackTrace();
				//cancel();
			}
		}
	}

	public Connection(ConnectionHandler handler) {
		this.handler = handler;
	}
	
	public void startPingTask() {
		if (pingFuture != null) return;
		pingFuture = Rukkit.getThreadManager().schedule(new PingTasker(), 2000, 2000);
	}
	
	public void startTeamTask() {
		if (teamFuture != null) return;
		teamFuture = Rukkit.getThreadManager().schedule(new TeamTasker(), 1000, 1000);
	}
	
	public void stopPingTask() {
		if (pingFuture == null) return;
		Rukkit.getThreadManager().shutdownTask(pingFuture);
		pingFuture = null;
	}
	
	public void stopTeamTask() {
		if (teamFuture == null) return;
		Rukkit.getThreadManager().shutdownTask(teamFuture);
		teamFuture = null;
	}

	public void sendChat(String msg) {
		try {
			Rukkit.getConnectionManager().broadcast(Packet.chat(player.name, msg, player.playerIndex));
		} catch (IOException ignored) {}
	}

	public void sendServerMessage(String msg) {
		try {
			handler.ctx.writeAndFlush(Packet.chat("SERVER", msg, -1));
		} catch (IOException e) {}
	}
	
	public void sendMessage(String from, String msg, int team) {
		try {
			handler.ctx.writeAndFlush(Packet.chat(from, msg, team));
		} catch (IOException e) {}
	}
	
	public void kick(String reason) {
		try {
			handler.ctx.writeAndFlush(Packet.kick(reason));
		} catch (IOException e) {}
	}
	
	public void pong() {
		player.ping = (int) (System.currentTimeMillis() - pingTime);
	}
}
