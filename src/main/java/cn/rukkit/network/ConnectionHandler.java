/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network;
import cn.rukkit.*;
import cn.rukkit.event.*;
import cn.rukkit.event.action.*;
import cn.rukkit.event.player.*;
import cn.rukkit.event.server.ServerQuestionRespondEvent;
import cn.rukkit.game.*;
import cn.rukkit.game.unit.*;
import cn.rukkit.network.command.*;
import cn.rukkit.network.packet.*;
import cn.rukkit.util.LangUtil;
import cn.rukkit.util.MathUtil;
import io.netty.channel.*;

import java.io.DataInputStream;
import java.util.concurrent.*;
import org.slf4j.*;
import io.netty.util.ReferenceCountUtil;

public class ConnectionHandler extends ChannelInboundHandlerAdapter {
	Logger log = LoggerFactory.getLogger(ConnectionHandler.class);

	public ChannelHandlerContext ctx;
	private RoomConnection conn;
	private ScheduledFuture timeoutFuture;

	private NetworkRoom currentRoom;
	private String disconnectReason = "Unknown";
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
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		// TODO: Implement this method
		super.channelRegistered(ctx);
		// 保存 ctx 实例
		this.ctx = ctx;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		// TODO: Implement this method
		super.channelActive(ctx);
		startTimeout();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		// TODO: Implement this method
		super.channelInactive(ctx);
		// 连接正确才调用事件
		if (conn != null) {
			PlayerLeftEvent.getListenerList().callListeners(new PlayerLeftEvent(conn.player, disconnectReason));
			currentRoom.connectionManager.discard(conn);
			Rukkit.getGlobalConnectionManager().discard(conn);
			conn.stopPingTask();
			conn.stopTeamTask();
		} else {
			if (!(ctx.channel().remoteAddress().toString().contains("18.216.139.119") || ctx.channel().remoteAddress().toString().contains("192.241.156.189"))) { // Whitelist of official server
				log.warn("There is a unexpected connection at connection {}.", ctx.channel().remoteAddress());
			}
		}
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		// TODO: Implement this method
		//super.channelRead(ctx, msg);
		Packet p = (Packet) msg;
		GameInputStream in = new GameInputStream(p);
		switch (p.type) {
			case Packet.PACKET_PREREGISTER_CONNECTION:
				log.debug("New connection established:{}", ctx.channel().remoteAddress());
				ctx.write(p.preRegister());
				ctx.writeAndFlush(p.chat("SERVER", LangUtil.getString("rukkit.playerRegister"), -1));
				break;
			case Packet.PACKET_PLAYER_INFO:
				String packageName = in.readString();
				log.debug("Ints:" + in.readInt());
				int gameVersionCode = in.readInt();
				in.readInt();
				String playerName = in.readString();
				// 服务器密码判定
				in.readByte(); // 其实是 boolean
				// 如果上面那个为 true，则接下来读 String 做密码
				in.readString(); // packageName
				// 玩家固有的uuid，前提是verifyCode不改变 (在rw内叫clientKey)
				String uuid = in.readString();
				// 核心单位检查，用于判断玩家是否对客户端进行修改
				// 1.14:1198432602
				// 1.15:678359601
				int coreUnitCheck = in.readInt();
				String verifyResult = in.readString();
				log.debug(String.format("Got Player(package=%s, version=%d, name=%s, uuid=%s, coreUnit=%d",
									   packageName, gameVersionCode, playerName, uuid, coreUnitCheck));

				// 获取当前的可用房间
				NetworkRoom room = Rukkit.getRoomManager().getAvailableRoom();

				// 获取是否为断线玩家
				NetworkPlayer targetPlayer = Rukkit.getGlobalConnectionManager().getAllPlayerByUUID(uuid);
				// 是否启用同步
				if (targetPlayer != null) {
					if (Rukkit.getConfig().syncEnabled) {
						// 获取上次断线时的断线房间
						currentRoom = targetPlayer.getRoom();
						log.info("Found offline room {}", currentRoom.toString());
					} else {
						currentRoom = room;
					}
				} else {
					currentRoom = room;
				}

				// 无可用房间，踢出
				if (currentRoom == null) {
					ctx.writeAndFlush(Packet.kick(LangUtil.getString("rukkit.gameFull")));
					return;
				}

				if (!currentRoom.isGaming() && targetPlayer != null) { // 如果 room 不在游戏，说明该uuid玩家发起了重复连接
					log.info("Dup player {} (UUID={}) joined!", playerName, uuid);
					if (Rukkit.getConfig().isDebug) {
						log.info("You are in the debug mode, allowing this situation!");
						targetPlayer = null; // 释放来保证正确加入
					} else {
						ctx.writeAndFlush(Packet.kick("You are already in server!"));
						return;
					}
				}

				// 刷新房间信息
				ctx.writeAndFlush(Packet.serverInfo(currentRoom.config));

				// 创建 RoomConnection
				conn = new RoomConnection(this, currentRoom);
				// 玩家实例判断
				if (targetPlayer != null && Rukkit.getConfig().syncEnabled) {
					conn.player = targetPlayer;
					conn.player.name = playerName;
				} else {
					NetworkPlayer player = new NetworkPlayer(conn);
					player.name = playerName;
					player.uuid = uuid;
					conn.player = player;
				}

				// 检查房主
				if (currentRoom.connectionManager.size() <= 0) {
					conn.sendServerMessage(LangUtil.getString("rukkit.playerGotAdmin"));
					conn.player.isAdmin = true;
					ctx.writeAndFlush(Packet.serverInfo(currentRoom.config, true));
				} else {
					ctx.writeAndFlush(Packet.serverInfo(currentRoom.config));
				}

				// 当前房间是否在游戏
				if (currentRoom.isGaming()) {
					// 是否启用同步
					if (Rukkit.getConfig().syncEnabled) {
						log.info("Start Syncing!");
						stopTimeout();
						conn.player.updateServerInfo();
						currentRoom.connectionManager.set(conn, conn.player.playerIndex);
						conn.startTeamTask();
						conn.updateTeamList(false);
						conn.startPingTask();
						// Sync game
						conn.handler.ctx.writeAndFlush(Packet.startGame());
						// conn.handler.ctx.writeAndFlush(Packet.sendSave(currentRoom, Rukkit.getDefaultSave().arr, false));
						currentRoom.syncGame();
						conn.player.isDisconnected = false;

						// PlayerJoinEvent.getListenerList().callListeners(new PlayerJoinEvent(conn.player));
						PlayerReconnectEvent.getListenerList().callListeners(new PlayerReconnectEvent(conn.player));
					} else {
						ctx.writeAndFlush(p.kick(LangUtil.getString("rukkit.gameStarted")));
					}
				}

				//Adding into GlobalConnectionManager.
				Rukkit.getGlobalConnectionManager().add(conn);
				//Adding into RoomConnectionManager.
				if (targetPlayer == null) {
					currentRoom.connectionManager.add(conn);
				}

				//load player Data.
				try {
					conn.player.loadPlayerData();
				} catch (Exception e) {
					log.warn("Player {} data load failed!", playerName);
				}
				conn.sendServerMessage(LangUtil.getFormatString("rukkit.room", currentRoom.roomId));
				conn.sendServerMessage(Rukkit.getConfig().welcomeMsg
						.replace("{playerName}", playerName)
						.replace("{simpleUUID}", uuid.substring(0, 7))
						.replace("{packageName}", packageName)
						.replace("{versionCode}", String.valueOf(gameVersionCode)));

				if (targetPlayer == null) {
					conn.startPingTask();
					conn.startTeamTask();
					conn.updateTeamList(false);
					stopTimeout();
					PlayerJoinEvent.getListenerList().callListeners(new PlayerJoinEvent(conn.player));
				}

				break;
			case Packet.PACKET_HEART_BEAT_RESPONSE:
				conn.pong();
				break;
			case Packet.PACKET_ADD_CHAT:
				String chatmsg = in.readString();
				if (chatmsg.startsWith(".") || chatmsg.startsWith("-") || chatmsg.startsWith("_")) {
					Rukkit.getCommandManager().executeChatCommand(conn , chatmsg.substring(1));
				} else {
					if (PlayerChatEvent.getListenerList().callListeners(new PlayerChatEvent(conn.player, chatmsg))) {
						currentRoom.connectionManager.broadcast(p.chat(conn.player.name, chatmsg, conn.player.playerIndex));
					}
				}
				break;
			case Packet.PACKET_ADD_GAMECOMMAND:
				GameCommand cmd = new GameCommand();
				cmd.arr = in.getDecodeBytes();
				//conn.sendGameCommand(cmd);
				GameInputStream str = new GameInputStream(cmd.arr);
				GameOutputStream out = new GameOutputStream();
				Event act = null;
				boolean isCustom = false;
				byte index,byte1,byte2;
				long l1, l2, l3, l4;
				//log.debug(str.readByte());
				index = str.readByte();
				out.writeByte(index);
				log.debug("-- Command Recording --");
				log.debug("teamIndex=" + index);

				//是否为BasicAction
				if (str.readBoolean()) {
					log.debug("-- BasicGameAction --");
					out.writeBoolean(true);
					//游戏指令
					GameActions action = (GameActions) str.readEnum(GameActions.class);
					out.writeEnum(action);
					log.debug("Action=" + action);
					//sendPacket(ctx, new Packet().chat("Debug", "Your action is:" + action, -1));
					//对应单位id，如果为-1则无建造
					int n2 = str.readInt();
					out.writeInt(n2);
					log.debug("BuildUnit:" + n2);
					String targetUnit = "";
					if (n2 == -2) {
						targetUnit = str.readString();
						out.writeString(targetUnit);
						//((BuildAction)act).targetUnit = unit;
						log.debug("Custom=" + targetUnit);
						//sendPacket(ctx, new Packet().chat("Debug", "(External)You are building by builder:" + targetUnit, -1));
					}

					if (n2 != -1 && n2 != -2) {
						targetUnit = InternalUnit.units[n2];
						//sendPacket(ctx, new Packet().chat("Debug", "You are building by builder:" + InternalUnit.units[n2], -1));
					}

					//动作的目标位置
					float x = str.readFloat();
					out.writeFloat(x);
					//act.x = x;
					log.debug("" + x);
					float y = str.readFloat();
					out.writeFloat(y);
					//act.y = y;
					log.debug("" + y);
					//sendPacket(ctx, new Packet().chat("Debug", "Quered: x=" + x + " y=" + y, -1));

					//对应单位的id(单位把执行动作到..上)
					long targetUnitID = str.readLong();
					out.writeLong(targetUnitID);
					log.debug("TargetUnitID=" + targetUnitID);

					byte1 = str.readByte();
					float float1 = str.readFloat();
					float float2 = str.readFloat();
					boolean bool1 = str.readBoolean();
					boolean bool2 = str.readBoolean();
					boolean bool3 = str.readBoolean();
					log.trace("Byte1=" + byte1);
					log.trace("Float1=" + float1);
					log.trace("Float2=" + float2);
					log.trace("Boolean1=" + bool1);
					log.trace("Boolean2=" + bool2);
					log.debug("Boolean3=" + bool3);
					out.writeByte(byte1);
					out.writeFloat(float1);
					out.writeFloat(float2);
					out.writeBoolean(bool1);
					out.writeBoolean(bool2);
					out.writeBoolean(bool3);
					//1.14新增
					if (str.readBoolean()) {
						out.writeBoolean(true);
						String actionId = str.readString();
						log.debug("SPECIALACTIONID=" + actionId);
						out.writeString(actionId);
					} else {
						out.writeBoolean(false);
					}
					switch (action) {
						case BUILD:
							act = new BuildEvent(conn.player, x, y, targetUnitID, targetUnit);
							break;
						case MOVE:
							act = new MoveEvent(conn.player, x, y, targetUnitID);
							break;
					}
					log.debug("-- End BasicGameAction --");
				} else {
					out.writeBoolean(false);
				}
				//
				log.trace("CommandBlock ended.");


				boolean bool4,isCancel;
				bool4 = str.readBoolean(); // 未知
				isCancel = str.readBoolean();
				log.trace("Boolean4=" + bool4);
				//是否为取消操作
				log.debug("isCancel=" + isCancel);
				out.writeBoolean(bool4);
				out.writeBoolean(isCancel);

				int int1,int2;
				int1 = str.readInt(); // SpecialAction 对应的id
				int2 = str.readInt(); // 单位的攻击模式
				log.debug("SpecialActionId=" + int1);
				log.debug("AttackMode=" + int2);
				out.writeInt(int1);
				out.writeInt(int2);

				// 疑似和核弹等目标发射有关，设置集结点
				if (str.readBoolean()) {
					out.writeBoolean(true);
					log.trace("A readBoolean is true.");
					float f3,f4;
					f3 = str.readFloat();
					f4 = str.readFloat();
					log.trace("Float3=" + f3);
					log.trace("Float4=" + f4);
					out.writeFloat(f3);
					out.writeFloat(f4);
				} else {
					out.writeBoolean(false);
				}

				//未知
				boolean bool6 = str.readBoolean();
				log.trace("Boolean6=" + bool6);

				//批量执行单位
				int t = str.readInt();
				out.writeBoolean(bool6);
				out.writeInt(t);
				//批量执行的单位数量
				log.debug("UnitCount(Maybe)=" + t);
				//批量执行的单位ID
				for (int i = 0;i < t;i++) {
					long unitidInMatch = str.readLong();
					log.debug("UnitidInMatch(Maybe)=" + unitidInMatch);
					out.writeLong(unitidInMatch);
				}

				// 如果为True, 则为预执行命令
				if (str.readBoolean()) {
					out.writeBoolean(true);
					log.trace("A readBoolean is true.");
					byte2 = str.readByte(); //玩家
					log.trace("Byte2=" + byte2);
					out.writeByte(byte2);
				} else {
					out.writeBoolean(false);
				}

				// 与Ping有关
				float pingX = 0;
				float pingY = 0;
				if (str.readBoolean()) {
					out.writeBoolean(true);
					log.trace("Its a ping packet.");
					//float f5,f6;
					pingX = str.readFloat();
					pingY = str.readFloat();
					log.debug("PingX={} , PingY={}", pingX, pingY);
					out.writeFloat(pingX);
					out.writeFloat(pingY);
				} else {
					out.writeBoolean(false);
				}

				// 执行的单位id
				long l6 = str.readLong();
				log.debug("CheckUnitID=" + l6);
				out.writeLong(l6);

				//Build块（实际上叫做SpecialAction, 可能是ping，或者是build)
				String buildUnit = str.readString();
				log.debug("str(BuildUnit):" + buildUnit);
				if (!buildUnit.equals("-1")) {
					if (buildUnit.startsWith("c_6_")) {
						act = new PingEvent(conn.player, pingX, pingY, buildUnit);
					} else {
						act = new TaskEvent(conn.player, buildUnit, l6, isCancel);
					}
					/*act = new ProduceAction();
					 ((ProduceAction) act).targetUnit = buildUnit;*/
					//sendPacket(ctx, new Packet().chat("Debug", "You are building: " + buildUnit, -1));
				}
				out.writeString(buildUnit);

				// 为true时，单位完成除建造和修复外的所有路径行动
				boolean bool7 = str.readBoolean();
				log.trace("Boolean7=" + bool7);
				out.writeBoolean(bool7);

				// 能否进行玩家控制的关键代码,填写32767即可。
				short short1 = str.readShort();
				log.trace("Short1=" + short1);
				out.stream.writeShort(32767);

				// SystemAction 比较危险，可以默认舍弃
				if (str.readBoolean()) {
					log.trace("A readBoolean is true.");
					out.writeBoolean(true);
					str.readByte();
					out.writeByte(0);
					float f1,f2;
					int i1;
					f1 = str.readFloat();
					f2 = str.readFloat();
					i1 = str.readInt();
					log.trace("{}, {}, {}", f1, f2, i1);
					out.writeFloat(f1);
					out.writeFloat(f2);
					out.writeInt(i1);
				} else {
					out.writeBoolean(false);
				}

				int movementUnitCount = str.readInt();
				out.writeInt(movementUnitCount);
				log.debug("count={}", movementUnitCount);
				for (int i = 0;i < movementUnitCount;i++) {
					float sx, sy, ex, ey;
					long unitid = str.readLong();
					sx = str.readFloat();
					sy = str.readFloat();
					ex = str.readFloat();
					ey = str.readFloat();
					out.writeLong(unitid);
					out.writeFloat(sx);
					out.writeFloat(sy);
					out.writeFloat(ex);
					out.writeFloat(ey);
					//当前时间刻
					int timestamp = str.readInt();
					out.writeInt(timestamp);
					//单位类型（1陆，潜艇，跨悬崖，跨悬崖跨水，悬浮5)
					UnitType u = (UnitType) str.readEnum(UnitType.class);
					log.debug("id={} startPos=({}, {}), endPos=({}, {}), startStamp={}, unitType={}", unitid, sx, sy, ex, ey, timestamp, u);
					out.writeEnum(u);

					// Path 有关内容
					if (str.readBoolean()) { // 是否存在path
						out.writeBoolean(true);
						//正式开始读取
						if (str.readBoolean()) { // 是否存在path
							out.writeBoolean(true);
							GzipEncoder outstr = out.getEncodeStream("p", true);
							byte[] bytes = str.getDecodeBytes();
							GzipDecoder dec = new GzipDecoder(bytes);
							DataInputStream ins = dec.stream;

							//path大小
							int pathCount = ins.readInt();
							outstr.stream.writeInt(pathCount);
							if (pathCount > 0) {
								short unitx = ins.readShort();
								short unity = ins.readShort();
								outstr.stream.writeShort(unitx);
								outstr.stream.writeShort(unity);
								log.trace("Start x:" + unitx + ", Start y:" + unity);
								for (int i2 = 1;i2 < pathCount;i2++) {
									// PathSize
									int len = ins.readByte();
									outstr.stream.writeByte(len);
									if (len < 128) {
										int i6 = (len & 3) - 1;
										int i7 = ((len & 12) >> 2) - 1;
										boolean bool = MathUtil.abs(i6) > 1 || MathUtil.abs(i7) > 1;
										if (bool) {
											log.warn("Bad unit path.");
										}
										unitx = (short)(unitx + i6);
										unity = (short)(unity + i7);
									} else {
										unitx = ins.readShort();
										unity = ins.readShort();
										outstr.stream.writeShort(unitx);
										outstr.stream.writeShort(unity);
									}
									//log.debug(ins.readShort());
								}
							}
							out.flushEncodeData(outstr);
						} else {
							out.writeBoolean(false);
						}
					} else {
						out.writeBoolean(false);
					}
				}

				boolean bool = str.readBoolean();
				out.writeBoolean(bool);
				log.debug("-- Command recording end --");

//				byte[] byt = new byte[str.stream.available()];
//				str.stream.read(byt);
//				out.stream.write(byt);
				Packet packet = out.createPacket(10);
				cmd.arr = packet.bytes;
				if (act != null) {
					ListenerList list = (ListenerList) act.getClass().getMethod("getListenerList").invoke(null);
					if (list.callListeners(act)) {
						conn.sendGameCommand(cmd);
					} else {
						log.debug("Event {} cancelled!", act);
					}
				} else {
					conn.sendGameCommand(cmd);
				}
				break;
			case Packet.PACKET_RANDY:
				currentRoom.connectionManager.broadcastServerMessage(String.format("Player '%s' is randy.", conn.player.name));
                break;
            case Packet.PACKET_SYNC:
                in.readByte();
                int frame = in.readInt();
                int time = in.readInt() / 15;
                log.debug("{}, {}, {}, {}", in.readFloat(), in.readFloat(), in.readBoolean(), in.readBoolean());
                byte[] save = new byte[in.stream.available()];
                in.stream.read(save);
                if (save.length > 20) {
                    SaveData data = new SaveData();
                    data.arr = save;
                    data.time = time;
                    conn.save = data;
                }
				break;
			case Packet.PACKET_SYNC_CHECKSUM_RESPONCE:
				in.readByte();
				int serverTick = in.readInt();
				int clientTick = in.readInt();
				log.info("[{}] Server tick: {}, Client tick: {}", conn.player.name, serverTick, clientTick);
				conn.lastSyncTick = clientTick;
				if (in.readBoolean()) {
					log.info("Player {} send checksum!", conn.player.name);
					in.readLong();
					in.readLong();
					DataInputStream din = in.getUnDecodeStream();
					din.readInt();
					int checkSumConut = din.readInt();
					log.debug("Total checksum: {}", checkSumConut);
					for (int i = 0;i < checkSumConut;i++) {
						din.readLong();
						long clientCheckData = din.readLong();
						log.trace("{}: client={}", conn.player.checkList.get(i).getDescription(), clientCheckData);
						conn.player.checkList.get(i).setCheckData(clientCheckData);
					}
					conn.currectRoom.checkSumReceived.incrementAndGet();
					conn.checkSumSent = true;
					synchronized (conn.currectRoom.checkSumReceived) {
						conn.currectRoom.checkSumReceived.notifyAll();
					}
				} else {
					log.info("Player {} did'n send checksum!We can sent back again!", conn.player.name);
					conn.doChecksum();
				}

				break;
			case Packet.PACKET_DISCONNECT:
				String reason = in.readString();
				disconnectReason = reason;
				// Disconnects gracefully.
				ctx.disconnect();
				break;
			case Packet.PACKET_QUESTION_RESPONCE:
				in.readByte(); //Always 1;
				int qid = in.readInt();
				String response = in.readString();
				ServerQuestionRespondEvent.getListenerList().callListeners(new ServerQuestionRespondEvent(conn.player, qid, response));
		}
		ReferenceCountUtil.release(msg);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		//super.exceptionCaught(ctx, cause)
		log.warn("Exception happened", cause);
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
