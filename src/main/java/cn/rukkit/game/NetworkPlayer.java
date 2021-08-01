package cn.rukkit.game;
import cn.rukkit.*;
import cn.rukkit.network.*;
import java.io.*;

public class NetworkPlayer
{
	public String name = "Unnamed";
	public String uuid;
	public int verifyCode = 114514;
	public int credits = 4000;
	public int team = 0;
	public int playerIndex;
	//public index;
	// Preparing for 1.15
	public int startingUnit;

	private Connection connection;

	public int ping;
	public boolean isAdmin = false;

	public boolean isAI;

	public boolean isSharingControl;

	public boolean isSurrounded;
	
	public NetworkPlayer(Connection connection) {
		this.connection = connection;
	}
	
	public Connection getConnection() {
		return this.connection;
	}
	
	public void writePlayer(DataOutputStream stream) throws IOException {
		if (Rukkit.getGameServer().isGaming()) {
			stream.writeByte(0);
			stream.writeInt(ping);
			stream.writeBoolean(true);
			stream.writeBoolean(true);
		} else {
			//玩家位置
			stream.writeByte(playerIndex);
			//玩家资金(毫无作用)
			stream.writeInt(credits);
			//玩家队
			stream.writeInt(team);

			stream.writeBoolean(true);

			if(isAdmin){
				stream.writeUTF("[[[" + name + "]]]");
			}else{
				stream.writeUTF(name);
			}
			stream.writeBoolean(true);

			//enc.stream.writeBoolean(true);
			stream.writeInt(ping);
			stream.writeLong(System.currentTimeMillis());

			//是否AI
			stream.writeBoolean(isAI);
			//AI难度
			stream.writeInt(0);

			//玩家队伍
			stream.writeInt(team);
			stream.writeByte(0);

			//分享控制
			stream.writeBoolean(isSharingControl);
			//是否掉线
			stream.writeBoolean(false);

			//是否投降
			stream.writeBoolean(isSurrounded);
			stream.writeBoolean(false);
			stream.writeInt(-9999);

			stream.writeBoolean(false);
			//是否房主
			stream.writeInt(0);
		}
	}

	public boolean movePlayer(int index){
		if (index > Rukkit.getConfig().maxPlayer) return false;
		PlayerManager playerGroup = Rukkit.getConnectionManager().getPlayerManager();
		for(NetworkPlayer p : playerGroup.getPlayerArray()){
			try{
				if(p.playerIndex == index){
					return false;
				}
			}catch(NullPointerException e){continue;}
		}
		this.playerIndex = index;
		playerGroup.remove(this);
		playerGroup.set(index, this);
		return true;
	}

	public boolean moveTeam(int team){
		if(team > 9 && team < 0){
			return false;
		} else {
			this.team = team;
		}
		return true;
	}

	public boolean giveAdmin(int index){
		NetworkPlayer player = Rukkit.getConnectionManager().getPlayerManager().get(index);
		if(index < 9 && index >= 0 && player != null && this.isAdmin){
			player.isAdmin = true;
			this.isAdmin = false;
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return "NetworkPlayer{" +
				"name='" + name + '\'' +
				", uuid='" + uuid + '\'' +
				", team=" + team +
				", playerIndex=" + playerIndex +
				", ping=" + ping +
				", isAdmin=" + isAdmin +
				", isSharingControl=" + isSharingControl +
				", isSurrounded=" + isSurrounded +
				'}';
	}

	public boolean isNull() {
		return false;
	}
}
