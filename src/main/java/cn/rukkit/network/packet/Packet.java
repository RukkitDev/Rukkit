package cn.rukkit.network.packet;

import cn.rukkit.*;
import cn.rukkit.game.map.CustomMapLoader;
import cn.rukkit.game.mod.Mod.*;
import cn.rukkit.network.*;
import cn.rukkit.network.command.*;
import cn.rukkit.util.*;
import java.io.*;
import java.util.*;

public class Packet {
	//Server Commands
	public static final int PACKET_REGISTER_CONNECTION = 161;
	public static final int PACKET_TEAM_LIST = 115;
	public static final int PACKET_HEART_BEAT = 108;
	public static final int PACKET_SEND_CHAT = 141;
	public static final int PACKET_SERVER_INFO = 106;
	public static final int PACKET_START_GAME = 120;
	public static final int PACKET_QUESTION = 117;
	public static final int PACKET_QUESTION_RESPONCE = 118;

	//Client Commands
	public static final int PACKET_PREREGISTER_CONNECTION = 160;
	public static final int PACKET_HEART_BEAT_RESPONSE = 109;
	public static final int PACKET_ADD_CHAT = 140;
	public static final int PACKET_PLAYER_INFO = 110;
	public static final int PACKET_DISCONNECT = 111;
	public static final int PACKET_RANDY = 112;

	//Game Commands
	public static final int PACKET_ADD_GAMECOMMAND = 20;
	public static final int PACKET_TICK = 10;
	public static final int PACKET_SYNC_CHECKSUM = 30;
	public static final int PACKET_SYNC_CHECKSUM_RESPONCE = 31;

	public byte[] bytes;
    public int type;

    public Packet(int type) {
        this.type = type;
    }

	public Packet() {
		this.type = 0;
	}

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
		return o2.createPacket(PACKET_SEND_CHAT);
	}

	/**
	 * Send a ping packet.
	 */
	public static Packet ping() throws IOException {
		GameOutputStream o = new GameOutputStream();
		o.writeLong(new Random().nextLong());
		o.writeByte(0);
		Packet p = o.createPacket(PACKET_HEART_BEAT);
		return p;
	}

	/**
	 * Send a preRegister packet to Client.
	 * Server UUID and verfiy code will be sended.
	 */
	public static Packet preRegister() throws IOException {
		//协议版本？
		GameOutputStream o = new GameOutputStream();
		o.writeString("io.rukkit");
		o.writeInt(1);
		o.writeInt(151);
		o.writeInt(151);
		o.writeString("io.rukkit");
		o.writeString("0000-000000000000");
		o.writeInt(114514);
		return o.createPacket(PACKET_REGISTER_CONNECTION);
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
	public static Packet gameCommand(int tick, GameCommand cmd) throws IOException {
		GameOutputStream o = new GameOutputStream();
		o.writeInt(tick);
		o.writeInt(1);
		o.startBlock("c", false);
		o.stream.write(cmd.arr);
		//o.stream.write(cmd.arr);
		o.endBlock();
		return (o.createPacket(10));
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
		return o.createPacket(10);
	}

	/*public Packet gameStart() throws IOException{
	 GameOutputStream o = new GameOutputStream();
	 o.writeByte(0);
	 //应该是Type
	 if(Rukkit.getRoundConfig().mapType == 0){
	 o.writeInt(0);
	 o.writeString("maps/skirmish/" + Rukkit.getRoundConfig().mapName + ".tmx");
	 }else if(Rukkit.getRoundConfig().mapType == 1){
	 o.writeInt(1);
	 o.writeFile(CustomMapLoader.getStreamByName(Rukkit.getRoundConfig().mapName + ".tmx"));
	 o.writeString(Rukkit.getRoundConfig().mapName + ".tmx");
	 }
	 o.writeBoolean(false);
	 return (o.createPacket(120));
	 }*/

	 /*public Packet serverInfoWithUnit(ArrayList<ModUnit> units, boolean bool) throws IOException{
	 ArrayList<ModUnit> li = Rukkit.getInternalModUnitsList();
	 li.addAll(Rukkit.getModManager().fetchAllEnabledModUnits());
	 li.addAll(units);
	 return serverInfo(bool, li);
	 }*/

	 public static Packet serverInfo() throws IOException{
		 return serverInfo(false, Rukkit.getModManager().fetchAllEnabledModUnits());
	 }

	 public static Packet serverInfo(Boolean isAdmin) throws IOException {
		 return serverInfo(isAdmin, Rukkit.getModManager().fetchAllEnabledModUnits());
	 }
	 

	public static Packet serverInfo(boolean isAdmin, ArrayList<ModUnit> units) throws IOException {
		GameOutputStream o = new GameOutputStream();
		o.writeString("com.corrodinggames.rts");
		//协议版本
		o.writeInt(151);
		//地图类型 0=官方 1=自定义 2=保存的游戏
		o.writeInt(Rukkit.getRoundConfig().mapType);
		o.writeString(Rukkit.getRoundConfig().mapName);
		//资金
		o.writeInt(GameUtils.getMoneyFormat(Rukkit.getRoundConfig().credits));
		o.writeInt(Rukkit.getRoundConfig().fogType);
		o.writeBoolean(true);
		o.writeInt(1);
		o.writeByte(4);
		o.writeBoolean(false);
		//是否为管理
		o.writeBoolean(isAdmin);
		o.writeInt(250);
		o.writeInt(250);

		o.writeInt(Rukkit.getRoundConfig().startingUnits);
		o.writeFloat(Rukkit.getRoundConfig().income);
		//禁核
		o.writeBoolean(Rukkit.getRoundConfig().disableNuke);
		o.writeBoolean(false);
		o.writeBoolean(true);

	
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
		for(ModUnit c: units){
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
		if(Rukkit.getRoundConfig().mapType == 0){
			o.writeInt(0);
			o.writeString("maps/skirmish/" + Rukkit.getRoundConfig().mapName + ".tmx");
		}else if(Rukkit.getRoundConfig().mapType == 1){
			o.writeInt(1);
			o.writeFile(CustomMapLoader.getStreamByName(Rukkit.getRoundConfig().mapName + ".tmx"));
			o.writeString(Rukkit.getRoundConfig().mapName + ".tmx");
		}
		o.writeBoolean(false);
		return (o.createPacket(120));
	}

	/*public Packet question(Player p, String question, ServerQuestionCallback callback) throws IOException{
	 int qid = ServerQuestionHandler.addQuestion(new ServerQuestion(p, callback));
	 GameOutputStream o = new GameOutputStream();
	 o.writeByte(1);
	 o.writeInt(qid);
	 o.writeString(question);
	 return (o.createPacket(PacketType.PACKET_QUESTION));
	 }*/

	/**
	 * Send a kick packet to client.
	 * @param reason Kick reason.
	 */
	public static Packet kick(String reason) throws IOException {
		GameOutputStream o = new GameOutputStream();
		o.writeString(reason);
		return (o.createPacket(150));
	}

}
