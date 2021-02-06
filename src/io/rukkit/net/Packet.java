package io.rukkit.net;
import io.rukkit.*;
import io.rukkit.command.*;
import io.rukkit.command.ServerQuestionHandler.*;
import io.rukkit.entity.*;
import io.rukkit.map.*;
import io.rukkit.util.*;
import java.io.*;
import java.util.*;

public class Packet
{
	public byte[] bytes;
    public int type;

    public Packet(int type) {
        this.type = type;
    }
	
	public Packet(){
		this.type = 0;
	}
	
	public GameInputStream gotGameInputStream(){
		return new GameInputStream(this);
	}
	
	public Packet chat(String from,String msg,int team) throws IOException{
		GameOutputStream o2 = new GameOutputStream();
		o2.writeString(msg);
		o2.writeByte(3);
		o2.writeBoolean(true);
		o2.writeString(from);
		o2.writeInt(team);
		o2.writeInt(team);
		return o2.createPacket(PacketType.PACKET_SEND_CHAT);
	}
	
	public Packet ping() throws IOException{
		GameOutputStream o = new GameOutputStream();
		o.writeLong(new Random().nextLong());
		o.writeByte(0);
		Packet p = o.createPacket(108);
		return p;
	}
	
	public Packet preRegister() throws IOException{
		//协议版本？
		GameOutputStream o = new GameOutputStream();
		o.writeString("io.rukkit");
		o.writeInt(1);
		o.writeInt(151);
		o.writeInt(151);
		o.writeString("io.rukkit");
		o.writeString(ServerProperties.UUID);
		o.writeInt(114514);
		return o.createPacket(PacketType.PACKET_REGISTER_CONNECTION);
	}
	
	public Packet teamInfo(){
		return null;
	}
	
	public Packet gameCommand(int tick, GameCommand cmd) throws IOException{
		GameOutputStream o = new GameOutputStream();
		o.writeInt(tick);
		o.writeInt(1);
		GzipEncoder enc = o.getEncodeStream("c", false);
		enc.stream.write(cmd.arr);
		//o.stream.write(cmd.arr);
		o.flushEncodeData(enc);
		return (o.createPacket(10));
	}
	
	public Packet emptyCommand(int tick) throws IOException{
		// TODO: Implement ths method
			GameOutputStream o = new GameOutputStream();
			o.writeInt(tick);
			o.writeInt(0);
			return o.createPacket(10);
	}
	
	public Packet gameStart() throws IOException{
		GameOutputStream o = new GameOutputStream();
		o.writeByte(0);
		//应该是Type
		if(ServerProperties.mapType == 0){
			o.writeInt(0);
			o.writeString("maps/skirmish/" + ServerProperties.mapName + ".tmx");
		}else if(ServerProperties.mapType == 1){
			o.writeInt(1);
			o.writeFile(CustomMapLoader.getStreamByName(ServerProperties.mapName + ".tmx"));
			o.writeString(ServerProperties.mapName + ".tmx");
			}
		o.writeBoolean(false);
		return (o.createPacket(120));
	}
	public Packet serverInfoWithUnit(ArrayList<ModUnit> units, boolean bool) throws IOException{
		ArrayList<ModUnit> li = Rukkit.getInternalModUnitsList();
		li.addAll(Rukkit.getModManager().fetchAllEnabledModUnits());
		li.addAll(units);
		return serverInfo(bool, li);
	}
	
	public Packet serverInfo() throws IOException{
		ArrayList<ModUnit> li = Rukkit.getInternalModUnitsList();
		li.addAll(Rukkit.getModManager().fetchAllEnabledModUnits());
		return serverInfo(false, li);
	}
	
	public Packet serverInfo(Boolean isAdmin) throws IOException {
		ArrayList<ModUnit> li = Rukkit.getInternalModUnitsList();
		li.addAll(Rukkit.getModManager().fetchAllEnabledModUnits());
		return serverInfo(isAdmin, li);
	}
	
	public Packet serverInfo(boolean isAdmin, ArrayList<ModUnit> units) throws IOException{
		//return emptyCommand(0);
		GameOutputStream o = new GameOutputStream();
		o.writeString("com.corrodinggames.rts");
		//协议版本
		o.writeInt(151);
		//地图类型 0=官方 1=自定义 2=保存的游戏
		o.writeInt(ServerProperties.mapType);
		o.writeString(ServerProperties.mapName);
		//资金
		o.writeInt(GameUtil.getMoneyFormat(ServerProperties.credits));
		o.writeInt(ServerProperties.fogType);
		o.writeBoolean(true);
		o.writeInt(1);
		o.writeByte(4);
		o.writeBoolean(false);
		//是否为管理
		o.writeBoolean(isAdmin);
		o.writeInt(250);
		o.writeInt(250);

		o.writeInt(ServerProperties.startingUnits);
		o.writeFloat(ServerProperties.income);
		//禁核
		o.writeBoolean(ServerProperties.disableNuke);
		o.writeBoolean(false);
		o.writeBoolean(true);

		GzipEncoder out = o.getEncodeStream("customUnits", false);
		out.stream.writeInt(1);
		//BufferedReader reader = new BufferedReader(new InputStreamReader(Packet.this.getClass().getClassLoader().getResourceAsStream("unitmeta.conf")));
		//LinkedList<String> li = new LinkedList<String>();
		/*String b = null;
		while ((b = reader.readLine()) != null)
		{
			li.addLast(b);
		}*/
		out.stream.writeInt(units.size());
		for(ModUnit c: units){
			//String unitdata[] = c.split("%#%");
			out.stream.writeUTF(c.getUnitName());
			out.stream.writeInt(c.getUnitId());
			out.stream.writeBoolean(true);
			out.stream.writeBoolean(false);
			/*try{
			if(!unitdata[2].equals("default")){
				out.stream.writeBoolean(true);
				out.stream.writeUTF(unitdata[2]);
			}else{
				out.stream.writeBoolean(false);
			}}
			catch(ArrayIndexOutOfBoundsException e){
				new Logger("ModLoader").w("You are using old unitmeta.Use new unitmeta.conf to support MODs.");
			}*/
			out.stream.writeLong(0);
			out.stream.writeLong(0);
		}
		
		/*for(ModUnit c :Rukkit.getModManager().fetchAllEnabledModUnits()) {
			out.stream.writeUTF(c.getUnitName());
			out.stream.writeInt(c.getUnitId());
			out.stream.writeBoolean(true);
			out.stream.writeBoolean(true);
			out.stream.writeUTF(c.getModName());
			out.stream.writeLong(0);
			out.stream.writeLong(0);
		}*/

		o.flushEncodeData(out);

		o.writeBoolean(false);
		o.writeBoolean(false);
		o.writeBoolean(false);
		
		
		return (o.createPacket(106));
	}
	
	public Packet sandSave() throws IOException{
		GameOutputStream o = new GameOutputStream();
		o.writeString("");
		return (o.createPacket(150));
	}
	
	public Packet question(Player p, String question, ServerQuestionCallback callback) throws IOException{
		int qid = ServerQuestionHandler.addQuestion(new ServerQuestion(p, callback));
		GameOutputStream o = new GameOutputStream();
		o.writeByte(1);
		o.writeInt(qid);
		o.writeString(question);
		return (o.createPacket(PacketType.PACKET_QUESTION));
	}
	
	public Packet kick(String reason) throws IOException{
		GameOutputStream o = new GameOutputStream();
		o.writeString(reason);
		return (o.createPacket(150));
	}
}
