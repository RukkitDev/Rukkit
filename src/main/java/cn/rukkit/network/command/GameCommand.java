/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.command;

import cn.rukkit.event.Event;
import cn.rukkit.event.action.BuildEvent;
import cn.rukkit.event.action.MoveEvent;
import cn.rukkit.event.action.PingEvent;
import cn.rukkit.event.action.TaskEvent;
import cn.rukkit.game.GameActions;
import cn.rukkit.game.UnitType;
import cn.rukkit.game.unit.InternalUnit;
import cn.rukkit.network.GameInputStream;
import cn.rukkit.network.GameOutputStream;
import cn.rukkit.network.GzipDecoder;
import cn.rukkit.network.GzipEncoder;
import cn.rukkit.util.MathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.IOException;

public class GameCommand
{
	class BasicGameAction {

	}
	public int sendBy;
	public byte[] arr;
	public long unitId;
	public Logger log = LoggerFactory.getLogger("GameCommand");

	public void readCommand() throws IOException {
		//conn.sendGameCommand(cmd);
		GameInputStream str = new GameInputStream(arr);
		GameOutputStream out = new GameOutputStream();
		Event act = null;
		boolean isCustom = false;
		byte index,byte1,byte2;
		long l1, l2, l3, l4;
		//log.debug(str.readByte());
		index = str.readByte();
		out.writeByte(index);
		log.debug("" + index);

		//是否为BasicAction
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
				log.debug("SPECIALACTIONID=" + actionId);
				out.writeString(actionId);
			} else {
				out.writeBoolean(false);
			}
			switch (action) {
				case BUILD:
					//act = new BuildEvent(conn.player, x, y, targetUnitID, targetUnit);
					break;
				case MOVE:
					//act = new MoveEvent(conn.player, x, y, targetUnitID);
					break;
			}
		} else {
			out.writeBoolean(false);
		}
		//
		log.debug("CommandBlock ended.");


		boolean bool4,isCancel;
		bool4 = str.readBoolean(); // 未知
		isCancel = str.readBoolean();
		log.debug("Boolean4=" + bool4);
		//是否为取消操作
		log.debug("Boolean5=" + isCancel);
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

		//未知
		boolean bool6 = str.readBoolean();
		log.debug("Boolean6=" + bool6);

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
			log.debug("A readBoolean is true.");
			byte2 = str.readByte(); //玩家
			log.debug("Byte2=" + byte2);
			out.writeByte(byte2);
		} else {
			out.writeBoolean(false);
		}

		// 与Ping有关
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

		// 作为UnitList检查的单位ID
		long l6 = str.readLong();
		log.debug("CheckUnitID=" + l6);
		out.writeLong(l6);

		//Build块（实际上叫做SpecialAction, 可能是ping，或者是build)
		String buildUnit = str.readString();
		log.debug("str(BuildUnit):" + buildUnit);
		if (!buildUnit.equals("-1")) {
			if (buildUnit.startsWith("c_6_")) {
				//act = new PingEvent(conn.player, pingX, pingY, buildUnit);
			} else {
				//act = new TaskEvent(conn.player, buildUnit, l6, isCancel);
			}
					/*act = new ProduceAction();
					 ((ProduceAction) act).targetUnit = buildUnit;*/
			//sendPacket(ctx, new Packet().chat("Debug", "You are building: " + buildUnit, -1));
		}
		out.writeString(buildUnit);

		// 为true时，单位完成除建造和修复外的所有路径行动
		boolean bool7 = str.readBoolean();
		log.debug("Boolean7=" + bool7);
		out.writeBoolean(bool7);

		// 能否进行玩家控制的关键代码,填写0即可。
		short short1 = str.readShort();
		log.debug("Short1=" + short1);
		out.stream.writeShort(0);

		// SystemAction 比较危险，可以默认舍弃
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

		str.stream.mark(str.stream.available());
		StringBuffer buf = new StringBuffer("Move units count: ");
		int movementUnitCount = str.readInt();
		out.writeInt(movementUnitCount);
		buf.append(movementUnitCount + " Unitids: ");
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
						log.debug("Start x:" + unitx + ", Start y:" + unity);
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
		log.debug(buf.toString());

		boolean bool = str.readBoolean();
		out.writeBoolean(bool);
	}
}
