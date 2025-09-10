package cn.rukkit.network.core.packet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import cn.rukkit.Rukkit;
import cn.rukkit.config.RoundConfig;
import cn.rukkit.game.CheckSumList;
import cn.rukkit.game.GameActions;
import cn.rukkit.game.PingType;
import cn.rukkit.game.CheckSumList.ChecksumItem;
import cn.rukkit.game.map.CustomMapLoader;
import cn.rukkit.game.mod.Mod.ModUnit;
import cn.rukkit.game.unit.InternalUnit;
import cn.rukkit.network.command.GameCommand;
import cn.rukkit.network.command.NewGameCommand;
import cn.rukkit.network.io.GameOutputStream;
import cn.rukkit.network.room.ServerRoom;
import cn.rukkit.util.GameUtils;

public class UniversalPacket {
	/**
	 * Send a player chat packet.
	 * @params from player who send it.
	 * @params team player team.Suggestion:-1=SERVER chat
	 */
	public static Packet chat(String from, String msg, int team) throws IOException {
		GameOutputStream o2 = new GameOutputStream();
		o2.writeString(msg);
		o2.writeByte(3);
		o2.writeBoolean(true);
		o2.writeString(from);
		o2.writeInt(team);
		o2.writeInt(team);
		return o2.createPacket(PacketType.SEND_CHAT);
	}

	/**
	 * Send a ping packet.
	 */
	public static Packet ping() throws IOException {
		GameOutputStream o = new GameOutputStream();
		o.writeLong(new Random().nextLong());
		o.writeByte(0);
		Packet p = o.createPacket(PacketType.HEART_BEAT);
		return p;
	}

	/**
	 * Send a preRegister packet to Client.
	 * Server UUID and verfiy code will be sended.
	 */
	public static Packet preRegister() throws IOException {
		//协议版本？(和游戏版本一致，协议向下兼容）
		GameOutputStream o = new GameOutputStream();
		o.writeString("cn.rukkit");
		o.writeInt(1);
		o.writeInt(176);
		o.writeInt(176);
		o.writeString("cn.rukkit");
		o.writeString(Rukkit.getConfig().UUID);
		o.writeInt(114514);
		o.writeInt(176);
		return o.createPacket(PacketType.REGISTER_CONNECTION);
	}

	/*public Packet teamInfo(Packet p, NetworkPlayer player) throws IOException{
	 GameOutputStream stream = new GameOutputStream();

	 return stream.createPacket(;
	 }*/

	/**
	 * Send a gameCommand packet to client.
	 * @params tick tickTime in game.
	 * @params cmd gameCommand content.
	 */
	public static Packet gameCommand(int tick, NewGameCommand cmd) throws IOException {
		GameOutputStream o = new GameOutputStream();
		o.writeInt(tick);
		o.writeInt(1);
		o.startBlock("c", false);
		o.stream.write(cmd.arr);
		//o.stream.write(cmd.arr);
		o.endBlock();
		return (o.createPacket(PacketType.TICK));
	}

	/**
	 * Send a empty gameCommand packet to client.
	 * Always used for keep game running.
	 * @params tick tickTime in game.
	 */
	public static Packet emptyCommand(int tick) throws IOException {
		// TODO: Implement ths method
		GameOutputStream o = new GameOutputStream();
		o.writeInt(tick);
		o.writeInt(0);
		return o.createPacket(PacketType.TICK);
	}

	public static Packet gameStart() throws IOException {
		GameOutputStream o = new GameOutputStream();
		o.writeByte(0);
		//应该是Type
		if (Rukkit.getRoundConfig().mapType == 0) {
			o.writeInt(0);
			o.writeString("maps/skirmish/" + Rukkit.getRoundConfig().mapName + ".tmx");
		} else if (Rukkit.getRoundConfig().mapType == 1) {
			o.writeInt(1);
			o.writeFile(CustomMapLoader.getStreamByName(Rukkit.getRoundConfig().mapName + ".tmx"));
			o.writeString(Rukkit.getRoundConfig().mapName + ".tmx");
		}
		o.writeBoolean(false);
		return (o.createPacket(PacketType.START_GAME));
	}

	/*public Packet serverInfoWithUnit(ArrayList<ModUnit> units, boolean bool) throws IOException{
	 ArrayList<ModUnit> li = Rukkit.getInternalModUnitsList();
	 li.addAll(Rukkit.getModManager().fetchAllEnabledModUnits());
	 li.addAll(units);
	 return serverInfo(bool, li);
	 }*/

	public static Packet serverInfo(RoundConfig config) throws IOException {
		return serverInfo(config, false, Rukkit.getModManager().fetchAllEnabledModUnits());
	}

	public static Packet serverInfo(RoundConfig config, Boolean isAdmin) throws IOException {
		return serverInfo(config, isAdmin, Rukkit.getModManager().fetchAllEnabledModUnits());
	}



	public static Packet serverInfo(RoundConfig config, boolean isAdmin, ArrayList<ModUnit> units) throws IOException {
		GameOutputStream o = new GameOutputStream();
		o.writeString("com.corrodinggames.rts");
		//协议版本
		o.writeInt(176);
		//地图类型 0=官方 1=自定义 2=保存的游戏
		o.writeInt(config.mapType);
		o.writeString(config.mapName);
		//资金
		o.writeInt(GameUtils.getMoneyFormat(config.credits));
		o.writeInt(config.fogType);
		o.writeBoolean(true); // revealedMap
		o.writeInt(1);
		o.writeByte(4);
		o.writeBoolean(false);
		//是否为管理
		o.writeBoolean(isAdmin);
		o.writeInt(Rukkit.getConfig().maxUnitsPerPlayer);
		o.writeInt(Rukkit.getConfig().maxUnitsPerPlayer);

		o.writeInt(config.startingUnits);
		o.writeFloat(config.income);
		//禁核
		o.writeBoolean(config.disableNuke);
		o.writeBoolean(false);
		o.writeBoolean(true); //是否为游戏房间，false则不读取下面的单位内容


		/*GzipEncoder out = o.getEncodeStream("customUnits", false);
		 out.stream.writeInt(1);
		 out.stream.writeInt(units.size());
		 for(ModUnit c: units){
		 out.stream.writeUTF(c.getUnitName());
		 out.stream.writeInt(c.getUnitId());
		 out.stream.writeBoolean(true);
		 out.stream.writeBoolean(false);
		 out.stream.writeLong(0);
		 out.stream.writeLong(0);
		 }
		 o.flushEncodeData(out);*/

		o.startBlock("customUnits", false);
		o.writeInt(1);
		o.writeInt(units.size());
		for (ModUnit c: units) {
			o.writeString(c.getUnitName());
			o.writeInt(c.getUnitId());
			o.writeBoolean(true);
			if (c.getModName().equals("default")) {
				o.writeBoolean(false);
			} else {
				o.writeBoolean(true);
				o.writeString(c.getModName());
			}
			o.writeLong(0);
			o.writeLong(0);
		}
		o.endBlock();

		o.writeBoolean(false);
		o.writeBoolean(false);
		o.writeBoolean(false);


		return (o.createPacket(106));
	}

	/**
	 * Send a GameSave packet to client.
	 * Useful for sync system.
	 * GameSave data.
	 */
	public static Packet sandSave() throws IOException {
		GameOutputStream o = new GameOutputStream();
		o.writeString("");
		return (o.createPacket(150));
	}

	public static Packet startGame() throws IOException {
		GameOutputStream o = new GameOutputStream();
		o.writeByte(0);
		//应该是Type
		if (Rukkit.getRoundConfig().mapType == 0) {
			o.writeInt(0);
			o.writeString("maps/skirmish/" + Rukkit.getRoundConfig().mapName + ".tmx");
		} else if (Rukkit.getRoundConfig().mapType == 1) {
			o.writeInt(1);
			o.writeFile(CustomMapLoader.getStreamByName(Rukkit.getRoundConfig().mapName + ".tmx"));
			o.writeString(Rukkit.getRoundConfig().mapName + ".tmx");
		}
		o.writeBoolean(false);
		return (o.createPacket(PacketType.START_GAME));
	}

	/*
	public Packet question(Player p, String question, ServerQuestionCallback callback) throws IOException{
	 int qid = ServerQuestionHandler.addQuestion(new ServerQuestion(p, callback));
	 GameOutputStream o = new GameOutputStream();
	 o.writeByte(1);
	 o.writeInt(qid);
	 o.writeString(question);
	 return (o.createPacket(PacketType.QUESTION));
	 } */

	/**
	 * Send a kick packet to client.
	 * @param reason Kick reason.
	 */
	public static Packet kick(String reason) throws IOException {
		GameOutputStream o = new GameOutputStream();
		o.writeString(reason);
		return (o.createPacket(150));
	}
    
    public static Packet sendSave(int step, byte[] bArr,boolean isPullSave) throws IOException {
        GameOutputStream out = new GameOutputStream();
        out.writeByte(0);
        out.writeInt(step);
        out.writeInt(step / 10);
        out.writeFloat((float) 1);
        out.writeFloat((float) 1);
        out.writeBoolean(isPullSave);
        out.writeBoolean(false);
        out.stream.write(bArr);
        Packet createPacket = out.createPacket(PacketType.SYNC);
        return createPacket;
    }

	public static Packet sendSave(ServerRoom room, byte[] bArr,boolean isPullSave) throws IOException {
		return sendSave(room.getCurrentStep(), bArr, isPullSave);
	}

    public static Packet sendPullSave(int step) throws IOException {
        GameOutputStream out = new GameOutputStream();
        out.writeByte(0);
        out.writeInt(step);
        out.writeInt(step / 10);
        out.writeFloat((float) 1);
        out.writeFloat((float) 1);
        out.writeBoolean(true);
        out.writeBoolean(false);
		out.startBlock("gameSave", false);
		out.write(Rukkit.getDefaultSave().arr);
		out.endBlock();
        Packet createPacket = out.createPacket(PacketType.SYNC);
        return createPacket;
    }

	public static Packet sendPullSave(int step,byte[] saveData) throws IOException {
        GameOutputStream out = new GameOutputStream();
        out.writeByte(0);
        out.writeInt(step);
        out.writeInt(step / 10);
        out.writeFloat((float) 1);
        out.writeFloat((float) 1);
        out.writeBoolean(true);
        out.writeBoolean(false);
		out.startBlock("gameSave", false);
		out.write(saveData);
		out.endBlock();
        Packet createPacket = out.createPacket(PacketType.SYNC);
        return createPacket;
    }

	public static Packet sendPullSave(ServerRoom room) throws IOException {
		return sendPullSave(room.getCurrentStep());
	}
	
	public static Packet syncCheckSum(int step,CheckSumList checkSumList) throws IOException {
		GameOutputStream out = new GameOutputStream();
		out.writeInt(step);//告诉玩家现在服务器的帧数
		out.writeLong(0);//回音
		out.writeInt(checkSumList.getCheckList().size());
		for (ChecksumItem item:checkSumList.getCheckList()) {
			out.writeLong(item.prefix);
		}
		return out.createPacket(PacketType.SYNC_CHECKSUM);
	}

	public static Packet gamePing(int step, int index, PingType type, float x, float y) throws IOException {
		GameOutputStream out = new GameOutputStream();
		out.writeInt(step);
		out.writeInt(1);
		out.startBlock("c", false);
		out.writeByte(index); // Team

		out.writeBoolean(false); //Command

		// 2 unknown booleans
		out.writeBoolean(false);
		out.writeBoolean(false);

		// 2 unknown ints
		out.writeInt(-1);
		out.writeInt(-1);

		out.writeBoolean(false);
		out.writeBoolean(false);

		// Unit count
		out.writeInt(0);

		// a true block
		out.writeBoolean(true);
		out.writeByte(0);

		//a true block
		out.writeBoolean(true);
		out.writeFloat(x);
		out.writeFloat(y);

		out.writeLong(-1);
		out.writeString("c_6_" + type.toString());

		out.writeBoolean(false);
		//通用结尾
		out.stream.writeShort(0);
		out.writeBoolean(false);
		out.writeInt(0);
		out.writeBoolean(false);
		out.endBlock();
		return out.createPacket(PacketType.TICK);
	}

	public static Packet gamePing(ServerRoom room, int index, PingType type, float x, float y) throws IOException {
		return gamePing(room.getCurrentStep(), index, type, x, y);
	}

	public static Packet gameSummon(int step, String unit, float x, float y, int team) throws IOException {
		GameOutputStream out = new GameOutputStream();
		out.writeInt(step);
		out.writeInt(1);
		out.startBlock("c", false);
		out.writeByte(team); // Team

		// COMMAND BLOCK
		out.writeBoolean(true); //Command
		out.writeEnum(GameActions.BUILD);
		int utype = -2;
		for (int i = 0;i < InternalUnit.units.length;i++) {
			if (InternalUnit.units[i].equals(unit)) {
				utype = i;
				break;
			}
		}
		out.writeInt(utype);
		if (utype == -2) {
			out.writeString(unit);
		}
		out.writeFloat(x);
		out.writeFloat(y);
		out.writeLong(-1L); //target uid
		out.writeByte(42);
		out.writeFloat(1.0f);
		out.writeFloat(1.0f);
		out.writeBoolean(false);
		out.writeBoolean(false);
		out.writeBoolean(false);
		//1.14 action
		out.writeBoolean(false);

		// 2 unknown booleans
		out.writeBoolean(false);
		out.writeBoolean(false);

		// 2 unknown ints
		out.writeInt(-1);
		out.writeInt(-1);

		out.writeBoolean(false);
		out.writeBoolean(false);

		// Unit count
		out.writeInt(0);

		// a block
		out.writeBoolean(false);

		//a block
		out.writeBoolean(false);

		out.writeLong(-1);
		out.writeString(unit);

		out.writeBoolean(false);
		//通用结尾
		out.stream.writeShort(0);
		// System action
		out.writeBoolean(true);
		out.writeByte(0);
		out.writeFloat(0);
		out.writeFloat(0);
		out.writeInt(5); //action type

		out.writeInt(0);
		out.writeBoolean(false);
		out.endBlock();
		return out.createPacket(PacketType.TICK);
	}

	public static Packet gameSummon(int step, String unit, float x, float y) throws IOException {
		return gameSummon(step, unit, x, y, -1);
	}

	public static Packet gameSummon(ServerRoom room, String unit, float x, float y) throws IOException {
		return gameSummon(room, unit, x, y, -1);
	}

	public static Packet gameSummon(ServerRoom room, String unit, float x, float y, int team) throws IOException {
		return gameSummon(room.getCurrentStep(), unit, x, y, team);
	}

	public static Packet gameSurrounder(int step, int index) throws IOException {
		GameOutputStream out = new GameOutputStream();
		out.writeInt(step);
		out.writeInt(1);
		out.startBlock("c", false);
		out.writeByte(index); // Team

		// COMMAND BLOCK
		out.writeBoolean(false); //Command

		// 2 unknown booleans
		out.writeBoolean(false);
		out.writeBoolean(false);

		// 2 unknown ints
		out.writeInt(-1);
		out.writeInt(-1);

		out.writeBoolean(false);
		out.writeBoolean(false);

		// Unit count
		out.writeInt(0);

		// a block
		out.writeBoolean(false);

		//a block
		out.writeBoolean(false);

		out.writeLong(-1);
		out.writeString("-1");

		out.writeBoolean(false);
		//通用结尾
		out.stream.writeShort(0);
		// System action
		out.writeBoolean(true);
		out.writeByte(0);
		out.writeFloat(0);
		out.writeFloat(0);
		out.writeInt(100); //action type

		out.writeInt(0);
		out.writeBoolean(false);
		out.endBlock();
		return out.createPacket(PacketType.TICK);
	}

	public static Packet gameSurrounder(ServerRoom room, int index) throws IOException {
		return gameSurrounder(room.getCurrentStep(), index);
	}

	public static Packet packetQuestion(int qid, String question) throws IOException {
		GameOutputStream out = new GameOutputStream();
		out.writeByte(1);
		out.writeInt(qid);
		out.writeString(question);
		return out.createPacket(PacketType.QUESTION);
	}

	public static Packet packetReturnToBattleroom() throws IOException {
		GameOutputStream out = new GameOutputStream();
		out.writeByte(0);
		return out.createPacket(PacketType.RETURN_TO_BATTLEROOM);
	}

    public static Packet fromRelayJumpsToAnotherServerInternalPacket(String address) throws IOException {
        GameOutputStream o = new GameOutputStream();
        // The message contained in the package
        o.writeByte(0);
        // Protocol version? (I don't know)
        o.writeInt(3);
        // Debug
        o.writeBoolean(false);
        // For
        o.writeInt(1);//指定下方List长度
        o.writeString(address);

        return o.createPacket(PacketType.PACKET_RECONNECT_TO);
    }
}
