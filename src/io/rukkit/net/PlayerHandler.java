package io.rukkit.net;
import io.netty.channel.*;
import io.netty.channel.group.*;
import io.netty.util.concurrent.*;
import io.rukkit.*;
import io.rukkit.command.*;
import io.rukkit.entity.*;
import io.rukkit.event.*;
import io.rukkit.game.*;
import io.rukkit.util.*;
import java.io.*;
import java.util.*;
import io.rukkit.event.player.*;
import io.rukkit.event.action.*;
import java.lang.reflect.*;

public class PlayerHandler extends ChannelInboundHandlerAdapter
{

	public Player getPlayer()
	{
		return player;
	}

	class HeartBeatTask extends TimerTask
	{

		private ChannelHandlerContext ctx;

		HeartBeatTask(ChannelHandlerContext ctx)
		{
			this.ctx = ctx;
		}

		@Override
		public void run()
		{
			if (ctx.isRemoved())
			{
				cancel();
			}
			// TODO: Implement this method
			GameOutputStream o = new GameOutputStream();
			try
			{
				//log.d("Send Heartbeat packet(...");
				o.writeLong(new Random().nextLong());
				o.writeByte(0);
				Packet p = o.createPacket(108);
				sendPacket(ctx, p);
				pingTime = System.currentTimeMillis();
			}
			catch (IOException e)
			{
				log.w("Cannot send Heartbeat.Client is disconnected.");
				cancel();
			}
		}
	};

	class TeamTask extends TimerTask
	{

		private ChannelHandlerContext ctx;
		public TeamTask(ChannelHandlerContext ctx)
		{
			this.ctx = ctx;
		}

		@Override
		public void run()
		{
			if (ctx.isRemoved())
			{
				cancel();
			}
			// TODO: Implement this method
			GameOutputStream o = new GameOutputStream();
			try
			{
				//log.d("Sending teamlist...");
				o.writeInt(player.playerIndex);
				o.writeInt(ServerProperties.maxPlayer); //最大玩家
				GzipEncoder enc = o.getEncodeStream("teams");

				for (int i =0;i < ServerProperties.maxPlayer;i++)
				{
					Player playerp = PlayerGroup.get(i);
					enc.stream.writeBoolean(playerp != null);
					if (playerp == null)
					{/*log.d("null");*/continue;}
					//log.d(playerp.playerName);
					enc.stream.writeInt(0);
					playerp.writePlayer(enc.stream);
				}
				o.flushEncodeData(enc);

				o.writeInt(ServerProperties.fogType);
				o.writeInt(GameUtil.getMoneyFormat(ServerProperties.credits));
				o.writeBoolean(true);
				//ai
				o.writeInt(1);
				//
				o.writeByte(4);
				o.writeInt(250);
				o.writeInt(250);

				//初始单位
				o.writeInt(ServerProperties.startingUnits);
				o.writeFloat(ServerProperties.income);
				o.writeBoolean(ServerProperties.disableNuke);
				o.writeBoolean(false);
				o.writeBoolean(false);
				o.writeBoolean(ServerProperties.sharedControl);

				Packet p = o.createPacket(PacketType.PACKET_TEAM_LIST);

				sendPacket(ctx, p);
			}
			catch (IOException e)
			{
				e.printStackTrace();
				cancel();
			}
		}
	};

	//Set a logger
	Logger log = new Logger("Player");

	//Set a channelGroup
	static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

	//Set a Heartbeat & Team Timer
	private Timer HeartBeatTimer = new Timer();
	private Timer TeamTaskTimer = new Timer();

	//Set a player
	private Player player;

	//Ping time
	private long pingTime = 10;

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception
	{
		super.channelActive(ctx);
		ChannelGroups.add(ctx.channel());
	}

	//When got the packet.
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
	{
		log.setTag(ctx.name());
		//Convert msg to Packet
		Packet bb = (Packet) msg;
		log.d(bb.type);
		if (bb.type > 200)
		{
			log.w("Bad packet. skipping...");
		}
		else
		{
			packetRead(ctx, bb);
		}

	}

	//When read complete.
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception
	{
		// TODO: Implement this method
		super.channelReadComplete(ctx);
		ctx.flush();
	}

	private void sendPacket(ChannelHandlerContext ctx, Packet p)
	{
		ctx.writeAndFlush(p);
	}

	private void packetRead(ChannelHandlerContext ctx, Packet p) throws IOException
	{
		GameInputStream in = new GameInputStream(p);
		switch (p.type)
		{
			case PacketType.PACKET_PREREGISTER_CONNECTION:
				log.d("Got the preRegister.");

				sendPacket(ctx, new Packet().preRegister());
				Packet p2 = new Packet().chat("Server", "You are logging to Rukkit server.", 0);
				sendPacket(ctx, new Packet().chat("Server", "This is a unstable beta build.Be carefully.", 0));
				sendPacket(ctx, p2);
				break;
			case PacketType.PACKET_PLAYER_INFO:
				log.d("Got the register request.");
				sendPacket(ctx, new Packet().serverInfo());
				log.d(in.readString());
				log.d(in.readInt());
				log.d(in.readInt());
				log.d(in.readInt());
				String name = in.readString();

				log.d(in.readByte());
				log.i(in.readString());
				//PlayerID
				String uuid = (in.readString());
				log.i("Player UUID: " + uuid);
				log.d(in.readInt());
				log.d(in.readString());
				if (ChannelGroups.size() >= ServerProperties.maxPlayer + 1)
				{
					sendPacket(ctx, new Packet().kick("No free slots in this server!\n服务器已满!"));
					ctx.disconnect();
					return;
				}
				if (GameServer.isGaming())
				{
					sendPacket(ctx, new Packet().kick("Game had already started!\n游戏已开始！\nPlayers left(剩余玩家):" + (ChannelGroups.size() - 1)));
					ctx.disconnect();
					return;
				}
				this.player = new Player(ctx);
				this.player.playerName = name;
				this.player.UUID = uuid;
				PlayerGroup.addWithTeam(this.player);
				log.d(PlayerGroup.getPlayers().length);
				if (PlayerGroup.size() <= 1)
				{
					this.player.isAdmin = true;
					sendPacket(ctx, new Packet().chat("Server", "You are the admin of this server, " + name + "!", 0));
					sendPacket(ctx, new Packet().serverInfo(true));
					//sendPacket(ctx, new Packet().serverInfo(true));
				}
				sendPacket(ctx, new Packet().chat("Server", "Welcome, " + name + "!", 0));
				PlayerJoinEvent.getListenerList().callListeners(new PlayerJoinEvent(player));
				//Rukkit.getCurrentPluginHandler().onPlayerJoined(player);
				/*
				 sendPacket(ctx, new Packet().question(this.player, "Welcome!", new ServerQuestionCallback() {

				 @Override
				 public void onReceive(Player p, String responce)
				 {
				 try
				 {
				 sendPacket(p.ctx, new Packet().chat("SERVER", "You message is: " + responce, -1));
				 }
				 catch (IOException e)
				 {
				 e.printStackTrace();
				 }
				 // TODO: Implement this method
				 }
				 }));*/
				HeartBeatTimer.schedule(new HeartBeatTask(ctx), 0, 2000);
				TeamTaskTimer.schedule(new TeamTask(ctx), 0, 1000);
				break;
			case PacketType.PACKET_HEART_BEAT_RESPONSE:
				player.ping = (int) (System.currentTimeMillis() - pingTime);
				//log.d("Responced." + player.ping + "ms");
				break;
			case PacketType.PACKET_ADD_CHAT:
				String msg = in.readString();
				//AFK判断
				try
				{
					if (ChatCommand.afkTask.from == this.player && ChatCommand.afkTask.isAfk)
					{
						ChatCommand.afkTask.breakDown();
					}
				}
				catch (NullPointerException e)
				{}

				if (msg.startsWith(".") || msg.startsWith("-") || msg.startsWith("_"))
				{
					ChatCommand.executeCommand(msg.substring(1), ctx, player);
				}
				else
				{
					ChannelGroups.broadcast(new Packet().chat(player.playerName, msg, player.playerIndex));
					PlayerChatEvent.getListenerList().callListeners(new PlayerChatEvent(player, msg));
				}
				break;
			case PacketType.PACKET_ADD_GAMECOMMAND:
				GameCommand cmd = new GameCommand();
				cmd.arr = in.getDecodeBytes();

				/*synchronized(GameServer.commandQuere){*/
				//ChannelGroups.broadcast();
				GameInputStream str = new GameInputStream(cmd.arr);
				Event act = null;
				//log.d(str.readByte());
				log.d(str.readByte());
				//是否为单位动作
				if (str.readBoolean())
				{
					//游戏指令
					GameActions action = (GameActions) str.readEnum(GameActions.class);
					log.d("Action=" + action);
					//sendPacket(ctx, new Packet().chat("Debug", "Your action is:" + action, -1));
					//对应单位id，如果为-1则无建造
					int n2 = str.readInt();
					log.d("BuildUnit:" + n2);
					String targetUnit = "";
					if (n2 == -2)
					{
						targetUnit = str.readString();
						//((BuildAction)act).targetUnit = unit;
						log.i("Custom=" + targetUnit);
						//sendPacket(ctx, new Packet().chat("Debug", "(External)You are building by builder:" + targetUnit, -1));
					}

					if (n2 != -1 && n2 != -2)
					{
						targetUnit = InternalUnit.units[n2];
						//sendPacket(ctx, new Packet().chat("Debug", "You are building by builder:" + InternalUnit.units[n2], -1));
					}

					//动作的目标位置
					float x = str.readFloat();
					//act.x = x;
					log.d(x);
					float y = str.readFloat();
					//act.y = y;
					log.d(y);
					//sendPacket(ctx, new Packet().chat("Debug", "Quered: x=" + x + " y=" + y, -1));

					//对应单位的id(单位把执行动作到..上)
					log.d("TargetUnitID=" + str.readLong());


					log.d("Byte1=" + str.readByte());
					log.d("Float1=" + str.readFloat());
					log.d("Float2=" + str.readFloat());
					log.d("Boolean1=" + str.readBoolean());
					log.d("Boolean2=" + str.readBoolean());
					log.d("Boolean3=" + str.readBoolean());
					//1.14新增
					if (str.readBoolean())
					{
						log.d("ACTIONID=" + str.readString());
					}
					switch (action)
					{
						case BUILD:
							act = new BuildEvent(player, x, y, targetUnit);
							break;
						case MOVE:
							act = new MoveEvent(player, x, y);
							break;
					}
				}
				//
				log.d("CommandBlock ended.");

				log.d("Boolean4=" + str.readBoolean());
				//是否为取消操作
				log.d("Boolean5=" + str.readBoolean());
				//log.d("Boolean12="+);
				/*
				 if (str.readBoolean()) {

				 }*/
				//log.d("Boolean14="+str.readBoolean());
				log.d("Int1=" + str.readInt());
				log.d("Int2=" + str.readInt());

				if (str.readBoolean())
				{
					log.d("A readBoolean is true.");
					log.d("Float3=" + str.readFloat());
					log.d("Float4=" + str.readFloat());
				}
				log.d("Boolean6=" + str.readBoolean());
				int t = str.readInt();
				log.d("UnitCount(Maybe)=" + t);
				for (int i = 0;i < t;i++)
				{
					log.d("UnitidInMatch(Maybe)=" + str.readLong());
				}
				if (str.readBoolean())
				{
					log.d("A readBoolean is true.");
					log.d("Byte2=" + str.readByte());
				}
				if (str.readBoolean())
				{
					log.d("A readBoolean is true.");
					log.d("Float5=" + str.readFloat());
					log.d("Float6=" + str.readFloat());
				}
				log.d("Long2=" + str.readLong());
				//Build块
				String buildUnit = str.readString();
				log.d("str(BuildUnit):" + buildUnit);
				if (!buildUnit.equals("-1"))
				{
					act = new TaskEvent(player, buildUnit);
					/*act = new ProduceAction();
					 ((ProduceAction) act).targetUnit = buildUnit;*/
					//sendPacket(ctx, new Packet().chat("Debug", "You are building: " + buildUnit, -1));
				}
				log.d("Boolean7=" + str.readBoolean());

				log.d("Short1=" + str.readShort());
				if (str.readBoolean())
				{
					log.d("A readBoolean is true.");
					str.readByte();
					log.d(str.readFloat());
					log.d(str.readFloat());
					log.d(str.readInt());
				}
				StringBuffer buf = new StringBuffer("Move units count: ");
				int n2 = str.readInt();
				buf.append(n2 + " Unitids: ");
				for (int i = 0;i < n2;i++)
				{
					long unitid = str.readLong();
					log.d("unitid:" + unitid);
					buf.append(unitid + " ");
					log.d("startx:" + str.readFloat());
					log.d("starty:" + str.readFloat());
					log.d("endx" + str.readFloat());
					log.d("endy" + str.readFloat());
					//当前时间刻
					log.d(str.readInt());
					//单位类型（1陆，潜艇，跨悬崖，跨悬崖跨水，悬浮5)
					Unit.UnitType u = (Unit.UnitType) str.readEnum(Unit.UnitType.class);
					buf.append("(" + u + ") ");
					log.d(u);
					//ppp
					if (str.readBoolean())
					{
						if (str.readBoolean())
						{
							DataInputStream ins = str.getDecodeStream();
							//跨过的图块大小
							int n3 = ins.readInt();
							log.d(n3);
							if (n3 > 0)
							{
								short unitx = ins.readShort();
								short unity = ins.readShort();
								log.d("Start x:" + unitx + ", Start y:" + unity);
								for (int i2 = 1;i2 < n3;i2++)
								{
									int len = ins.readByte();
									//log.d(len);
									//int i5 = 12;
									if (len < 128)
									{
										int i6 = (len & 3) - 1;
										int i7 = ((len & 12) >> 2) - 1;
										boolean bool = MathUtil.abs(i6) > 1 || MathUtil.abs(i7) > 1;
										if (bool)
										{
											log.w("Bad unit path.");
										}
										//log.d(i6);
										//log.d(i7);
										unitx = (short)(unitx + i6);
										unity = (short)(unity + i7);
										continue;
									}
									log.d(ins.readShort());
									log.d(ins.readShort());
								}
								log.d("End x:" + unitx + ", End y:" + unity);
							}
						}
					}

				}
				log.d(str.readBoolean());
				if (n2 != 0)
				{
					//sendPacket(ctx, new Packet().chat("Debug", buf.toString(), -1));
				}
				//GameServer.commandQuere.add(cmd);
				//}
				//if(Rukkit.getCurrentPluginManager().(player, act)){
				try
				{
					if (act != null)
					{
						ListenerList list = (ListenerList) act.getClass().getMethod("getListenerList").invoke(null);
						if (!list.callListeners(act))
						{
							log.d("action is false!");
						} else {
							synchronized (GameServer.commandQuere)
							{
								GameServer.commandQuere.add(cmd);
							}
						}
					} else {
						synchronized (GameServer.commandQuere)
						{
							GameServer.commandQuere.add(cmd);
						}
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				//}
				break;
			case PacketType.PACKET_QUESTION_RESPONCE:
				in.readByte();
				int qid = in.readInt();
				ServerQuestion question = ServerQuestionHandler.getQuestionById(qid);
				question.responce = in.readString();
				log.d("Question responce is: qid=" + qid + " " + "responce=" + question.responce);
				question.callback.onReceive(question.from, question.responce);
				ServerQuestionHandler.questionQuere.remove(question);
				break;
			case PacketType.PACKET_DISCONNECT:
				ctx.disconnect();

		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception
	{
		// TODO: Implement this method
		super.channelInactive(ctx);
		try
		{
			ChannelGroups.broadcast(new Packet().chat("Server", player.playerName + " disconnected!", -1));
			if (GameServer.isGaming())
			{
				ChatCommand.executeCommand("t Sharing control due to disconnect. 玩家离线，分享控制已开启", ctx, player);
				player.isSharingControl = true;
			}
			PlayerGroup.remove(this.player);
			if (player.isAdmin)
			{
				for (int i=0;i < PlayerGroup.getPlayers().length;i++)
				{
					try
					{
						PlayerGroup.get(i).isAdmin = true;
						ChannelGroups.broadcast(new Packet().serverInfo());
						PlayerGroup.get(i).ctx.writeAndFlush(new Packet().serverInfo(true));
						log.d("admin tranfered.");
						break;
					}
					catch (NullPointerException e)
					{continue;}
				}
			}
			if (ChatCommand.afkTask.to == player)
			{
				ChatCommand.afkTask.breakDown();
			}
		}
		catch (NullPointerException e)
		{}
		ChannelGroups.discard(ctx.channel());
	}



	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
	{
		// TODO: Implement this method
		super.exceptionCaught(ctx, cause);
		cause.printStackTrace();
	}

}
