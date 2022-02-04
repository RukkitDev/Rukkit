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
import cn.rukkit.game.*;
import cn.rukkit.game.unit.*;
import cn.rukkit.network.command.*;
import cn.rukkit.network.packet.*;
import io.netty.channel.*;
import java.util.concurrent.*;
import org.slf4j.*;
import io.netty.util.ReferenceCountUtil;

public class ConnectionHandler extends ChannelInboundHandlerAdapter {
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
		PlayerLeftEvent.getListenerList().callListeners(new PlayerLeftEvent(conn.player));
		Rukkit.getConnectionManager().discard(conn);
		conn.stopPingTask();
		conn.stopTeamTask();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		// TODO: Implement this method
		//super.channelRead(ctx, msg);
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
				log.debug("Ints:" + in.readInt());
				int gameVersionCode = in.readInt();
				in.readInt();
				String playerName = in.readString();
				in.readByte();
				in.readString();
				String uuid = in.readString();
				in.readInt();
				String verifyResult = in.readString();
				log.info(String.format("Got Player(package=%s, version=%d, name=%s, uuid=%s, verify=%s",
									   packageName, gameVersionCode, playerName, uuid, verifyResult));
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
				//Check admin.
				if (Rukkit.getConnectionManager().size() <= 0 && !Rukkit.getConfig().nonStopMode) {
					conn.sendServerMessage("You are the ADMIN of this server!");
					conn.player.isAdmin = true;
					ctx.writeAndFlush(Packet.serverInfo(true));
				} else {
                    ctx.writeAndFlush(Packet.serverInfo());
                }

				// Check gaming && no-stop mode.
				if (Rukkit.getGameServer().isGaming()) {
					if (Rukkit.getConfig().nonStopMode) {
						// No stop mode.
						// If is the first player
						if (Rukkit.getConnectionManager().size() <= 0) {
							// Check lastSaveData
							if (Rukkit.getGameServer().lastNoStopSave != null) {
								Rukkit.getConnectionManager().add(conn);
								stopTimeout();
								conn.startPingTask();
								conn.updateTeamList(false);
								PlayerJoinEvent.getListenerList().callListeners(new PlayerJoinEvent(conn.player));
								// Start game.
								conn.handler.ctx.writeAndFlush(Packet.startGame());
								// Send save
								conn.handler.ctx.writeAndFlush(Packet.sendSave(Rukkit.getGameServer().lastNoStopSave.arr, false));
								conn.startTeamTask();
								Rukkit.getGameServer().notifyGameTask();
								return;
							} else {
								Rukkit.getConnectionManager().add(conn);
								stopTimeout();
								conn.startPingTask();
								conn.updateTeamList(false);
								PlayerJoinEvent.getListenerList().callListeners(new PlayerJoinEvent(conn.player));
								// New Round!Start game.
								conn.handler.ctx.writeAndFlush(Packet.startGame());
								// Have a sync.
								Rukkit.getGameServer().syncGame();
								conn.startTeamTask();
								Rukkit.getGameServer().notifyGameTask();
								return;
							}
						} else {
							Rukkit.getConnectionManager().add(conn);
							stopTimeout();
							conn.startPingTask();
							conn.updateTeamList(false);
							PlayerJoinEvent.getListenerList().callListeners(new PlayerJoinEvent(conn.player));
							// New Round!Start game.
							conn.handler.ctx.writeAndFlush(Packet.startGame());
							// Have a sync.
							Rukkit.getGameServer().syncGame();
							conn.startTeamTask();
							Rukkit.getGameServer().notifyGameTask();
							return;
						}
					} else if (Rukkit.getConfig().syncEnabled) {
						// If sync enabled, get target player
						NetworkPlayer targetPlayer = Rukkit.getConnectionManager().getPlayerManager().getPlayerByUUID(uuid);
						// If player is a reconnecting player
						if (targetPlayer != null) {
							stopTimeout();
							Rukkit.getGameServer().syncGame();
							conn.player.playerIndex = targetPlayer.playerIndex;
							Rukkit.getConnectionManager().set(conn, targetPlayer.playerIndex);
							conn.updateTeamList(false);
							conn.startPingTask();
							// Sync game
							conn.handler.ctx.writeAndFlush(Packet.startGame());
							//conn.handler.ctx.writeAndFlush(Packet.sendSave(Rukkit.getGameServer().lastSave.arr, false));
							Rukkit.getGameServer().syncGame();
							conn.startTeamTask();
						} else {
							// kick
							ctx.writeAndFlush(p.kick("Game is started!"));
						}
						return;
					} else {
						// kick
						ctx.writeAndFlush(p.kick("Game is started!"));
						return;
					}
				}

				//Adding into ConnectionManager.
				Rukkit.getConnectionManager().add(conn);
				conn.startPingTask();
				conn.startTeamTask();
				conn.updateTeamList(false);
				stopTimeout();
				PlayerJoinEvent.getListenerList().callListeners(new PlayerJoinEvent(conn.player));
				break;
			case Packet.PACKET_HEART_BEAT_RESPONSE:
				conn.pong();
				break;
			case Packet.PACKET_ADD_CHAT:
				String chatmsg = in.readString();
				if (chatmsg.startsWith(".") || chatmsg.startsWith("-") || chatmsg.startsWith("_")) {
					Rukkit.getCommandManager().execute(conn , chatmsg.substring(1));
				} else {
					if (PlayerChatEvent.getListenerList().callListeners(new PlayerChatEvent(conn.player, chatmsg))) {
						Rukkit.getConnectionManager().broadcast(p.chat(conn.player.name, chatmsg, conn.player.playerIndex));
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
				log.debug("" + index);

				//是否为单位动作
				if (str.readBoolean()) {
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
						log.info("Custom=" + targetUnit);
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
					log.debug("Byte1=" + byte1);
					log.debug("Float1=" + float1);
					log.debug("Float2=" + float2);
					log.debug("Boolean1=" + bool1);
					log.debug("Boolean2=" + bool2);
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
						log.debug("ACTIONID=" + actionId);
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
				} else {
					out.writeBoolean(false);
				}
				//
				log.debug("CommandBlock ended.");

				boolean bool4,isCancel;
				bool4 = str.readBoolean();
				isCancel = str.readBoolean();
				log.debug("Boolean4=" + bool4);
				//是否为取消操作
				log.debug("Boolean5=" + isCancel);
				out.writeBoolean(bool4);
				out.writeBoolean(isCancel);
				//log.debug("Boolean12="+);
				/*
				 if (str.readBoolean()) {

				 }*/
				//log.debug("Boolean14="+str.readBoolean());

				int int1,int2;
				int1 = str.readInt();
				int2 = str.readInt();
				log.debug("Int1=" + int1);
				log.debug("Int2=" + int2);
				out.writeInt(int1);
				out.writeInt(int2);

				if (str.readBoolean()) {
					out.writeBoolean(true);
					log.debug("A readBoolean is true.");
					float f3,f4;
					f3 = str.readFloat();
					f4 = str.readFloat();
					log.debug("Float3=" + f3);
					log.debug("Float4=" + f4);
					out.writeFloat(f3);
					out.writeFloat(f4);
				} else {
					out.writeBoolean(false);
				}

				boolean bool6 = str.readBoolean();
				log.debug("Boolean6=" + bool6);
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

				if (str.readBoolean()) {
					out.writeBoolean(true);
					log.debug("A readBoolean is true.");
					byte2 = str.readByte();
					log.debug("Byte2=" + byte2);
					out.writeByte(byte2);
				} else {
					out.writeBoolean(false);
				}

				float pingX = 0;
				float pingY = 0;
				if (str.readBoolean()) {
					out.writeBoolean(true);
					log.debug("Its a ping packet.");
					//float f5,f6;
					pingX = str.readFloat();
					pingY = str.readFloat();
					log.debug("PingX=" + pingX);
					log.debug("PingX=" + pingY);
					out.writeFloat(pingX);
					out.writeFloat(pingY);

				} else {
					out.writeBoolean(false);
				}

				long l6 = str.readLong();
				log.debug("Long6=" + l6);
				out.writeLong(l6);
				//Build块
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

				boolean bool7 = str.readBoolean();
				log.debug("Boolean7=" + bool7);
				out.writeBoolean(bool7);

				short short1 = str.readShort();
				log.debug("Short1=" + short1);
				out.stream.writeShort(5);
				if (str.readBoolean()) {
					log.debug("A readBoolean is true.");
					out.writeBoolean(true);
					str.readByte();
					out.writeByte(0);
					float f1,f2;
					int i1;
					f1 = str.readFloat();
					f2 = str.readFloat();
					i1 = str.readInt();
					log.debug("{}, {}, {}", f1, f2, i1);
					out.writeFloat(f1);
					out.writeFloat(f2);
					out.writeInt(i1);
				} else {
					out.writeBoolean(false);
				}

				byte[] byt = new byte[str.stream.available()];
				str.stream.read(byt);
				out.stream.write(byt);
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
				// This code cause bugs, ignored.
				/*StringBuffer buf = new StringBuffer("Move units count: ");
				int n2 = str.readInt();
				buf.append(n2 + " Unitids: ");
				out.writeInt(n2);
				for (int i = 0;i < n2;i++) {
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
					log.debug("unitid:" + unitid);
					buf.append(unitid + " ");
					log.debug("startx:" + sx);
					log.debug("starty:" + sy);
					log.debug("endx" + ex);
					log.debug("endy" + ey);
					//当前时间刻
					int timestamp = str.readInt();
					log.debug("" + timestamp);
					out.writeInt(timestamp);
					//单位类型（1陆，潜艇，跨悬崖，跨悬崖跨水，悬浮5)
					UnitType u = (UnitType) str.readEnum(UnitType.class);
					buf.append("(" + u + ") ");
					log.debug(u.toString());
					out.writeEnum(u);
					//ppp
					if (str.readBoolean()) {
						if (str.readBoolean()) {
							byte[] bytes = str.getDecodeBytes();
							GzipDecoder dec = new GzipDecoder(bytes);
							DataInputStream ins = dec.stream;
							//跨过的图块大小
							int n3 = ins.readInt();
							log.debug(n3);
							if (n3 > 0) {
								short unitx = ins.readShort();
								short unity = ins.readShort();
								log.debug("Start x:" + unitx + ", Start y:" + unity);
								for (int i2 = 1;i2 < n3;i2++) {
									int len = ins.readByte();
									//log.debug(len);
									//int i5 = 12;
									if (len < 128) {
										int i6 = (len & 3) - 1;
										int i7 = ((len & 12) >> 2) - 1;
										boolean bool = MathUtil.abs(i6) > 1 || MathUtil.abs(i7) > 1;
										if (bool) {
											log.warn("Bad unit path.");
										}
										//log.debug(i6);
										//log.debug(i7);
										unitx = (short)(unitx + i6);
										unity = (short)(unity + i7);
										continue;
									}
									log.debug("{}, {}", ins.readShort(), ins.readShort());
									//log.debug(ins.readShort());
								}
								log.debug("End x:" + unitx + ", End y:" + unity);
							}
							GzipEncoder o = out.getEncodeStream("p", false);
							o.stream.write(bytes);
							out.flushEncodeData(o);
						} else {
							out.writeBoolean(false);
						}
					} else {
						out.writeBoolean(false);
					}

				}
				boolean bool8 = str.readBoolean();
				out.writeBoolean(bool8);
				log.debug("" + bool8);
				if (n2 != 0) {
					//sendPacket(ctx, new Packet().chat("Debug", buf.toString(), -1));
				}*/
				break;
			case Packet.PACKET_RANDY:
				conn.sendServerMessage(String.format("Player '%s' is randy.", conn.player.name));
                break;
            case Packet.PACKET_SYNC:
                in.readByte();
                in.readInt();
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
				conn.lastSyncTick = clientTick;
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
