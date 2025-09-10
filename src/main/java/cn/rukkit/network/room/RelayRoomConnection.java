/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.room;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;

import cn.rukkit.Rukkit;
import cn.rukkit.game.NetworkPlayer;
import cn.rukkit.game.SaveData;
import cn.rukkit.network.core.ConnectionHandler;
import cn.rukkit.network.core.packet.Packet;
import cn.rukkit.network.core.packet.PacketType;
import cn.rukkit.network.core.packet.UniversalPacket;
import cn.rukkit.network.io.GameOutputStream;
import cn.rukkit.network.io.GzipEncoder;
import cn.rukkit.util.GameUtils;

public class RelayRoomConnection {


	public NetworkPlayer player;
	public ConnectionHandler handler;
	public RelayNetworkRoom currentRoom;
	public long pingTime;
	public int lastSyncTick = 0;
	public boolean checkSumSent = false;
	public boolean isStartGame;
	public boolean syncFlag = true; 
	public int numberOfDesyncError = 0;
	public int ping = -1;


	private ScheduledFuture pingFuture;
	private ScheduledFuture teamFuture;
    public Packet cachePacket;//RW-HPS有 那就加上吧
	public String registerPlayerId;
	public String playerName;

	public void setCachePacket(Packet p){
		cachePacket = p;
	}





    public SaveData save;
	//public ChannelHandlerContext ctx;

	/**
	 * 心跳包任务
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
	 * 队伍列表任务
	 * TeamTask Scheduler.
	 */
	public class TeamTasker implements Runnable {
		@Override
		public void run() {
			// TODO: Implement this method
			try
			{
				updateTeamList();
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

	public RelayRoomConnection(ConnectionHandler handler) {
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

	/**
	 * 发送公开聊天
	 * @param msg
	 */
	public void sendChat(String msg) {
		try {
			currentRoom.connectionManager.broadcast(UniversalPacket.chat(player.name, msg, player.playerIndex));
		} catch (IOException ignored) {}
	}

	/**
	 * 发送服务器信息 ([SERVER])
	 * @param msg
	 */
	public void sendServerMessage(String msg) {
		try {
			handler.ctx.writeAndFlush(UniversalPacket.chat("SERVER", msg, -1));
		} catch (IOException e) {}
	}

	/**
	 * 发送玩家信息
	 * @param from 来源玩家名
	 * @param msg 信息
	 * @param team 队伍
	 */
	public void sendMessage(String from, String msg, int team) {
		try {
			handler.ctx.writeAndFlush(UniversalPacket.chat(from, msg, team));
		} catch (IOException e) {}
	}
	
	public void updateTeamList() throws IOException {
		updateTeamList(currentRoom.isGaming);
	}

	/**
	 * 更新队伍列表。
	 * @param simpleMode 简单模式(1.14+).减少网络数据通信。
	 * @throws IOException
	 */
	public void updateTeamList(boolean simpleMode) throws IOException {
		GameOutputStream o = new GameOutputStream();
		//log.d("Sending teamlist...");
		o.writeInt(player.playerIndex);
		// 1.14新增
		o.writeBoolean(simpleMode);
		o.writeInt(Rukkit.getConfig().maxPlayer); //maxPlayer
		//1.14启用Gzip压缩
		GzipEncoder enc = o.getEncodeStream("teams", true);

		for (int i =0;i < Rukkit.getConfig().maxPlayer;i++)
		{
			NetworkPlayer playerp = currentRoom.playerManager.get(i);

			enc.stream.writeBoolean(!playerp.isEmpty);

			// Ignore empty player
			if (playerp.isEmpty) {
				continue;
			}

			//1.14
			//enc.stream.writeByte(0);

			enc.stream.writeInt(255);
			playerp.writePlayer(enc.stream, simpleMode);
		}
		o.flushEncodeData(enc);

		o.writeInt(currentRoom.config.fogType);
		o.writeInt(GameUtils.getMoneyFormat(currentRoom.config.credits));
		o.writeBoolean(true);
		//ai
		o.writeInt(1);
		//
		o.writeByte(4);
		//maxUnit
		o.writeInt(250);
		o.writeInt(250);

		//初始单位
		o.writeInt(currentRoom.config.startingUnits);
		o.writeFloat(currentRoom.config.income);
		o.writeBoolean(currentRoom.config.disableNuke);
		o.writeBoolean(false);
		o.writeBoolean(false);
		o.writeBoolean(currentRoom.config.sharedControl);

		Packet p = o.createPacket(PacketType.TEAM_LIST);

		handler.ctx.writeAndFlush(p);
	}

	/**
	 * 踢出玩家
	 * @param reason 踢出理由
	 */
	public void kick(String reason) {
		try {
			handler.ctx.writeAndFlush(UniversalPacket.kick(reason));
		} catch (IOException e) {}
	}

	/**
	 * 心跳包返回
	 */
	public void pong() {
		ping = (int) (System.currentTimeMillis() - pingTime);
        handler.stopTimeout();
	}
}
