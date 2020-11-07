package io.rukkit.entity;

import io.rukkit.Rukkit;
import io.rukkit.util.*;
import java.io.*;
import java.util.*;
import io.netty.channel.*;
import io.rukkit.net.*;

public class Player
{
	public boolean isAdmin = false;
	public boolean isNull = true;
	public boolean isRandy = false;
	public boolean isSharingControl = false;
	public boolean isSurrounded = false;
	public String UUID = "";
	public String playerName = "unnamed";
	public int playerIndex = 0;
	public int playerTeam = 0;
	public int playerCredits = 4000;
	public int ping = 50;
	public ChannelHandlerContext ctx;
	
	public Player(ChannelHandlerContext ctx){
		this.ctx = ctx;
	}

	public void writePlayer(DataOutputStream stream) throws IOException{
		stream.writeByte(playerIndex);
		stream.writeInt(playerCredits);
		stream.writeInt(playerTeam);
		stream.writeBoolean(true);
		if(isAdmin){
			stream.writeUTF("[[[" + playerName + "]]]");
		}else{
			stream.writeUTF(playerName);
		}
		stream.writeBoolean(true);

		//enc.stream.writeBoolean(true);
		stream.writeInt(ping);
		stream.writeLong(System.currentTimeMillis());

		stream.writeBoolean(false);
		stream.writeInt(0);

		stream.writeInt(playerIndex);
		stream.writeByte(0);
		//分享控制
		stream.writeBoolean(isSharingControl);
		stream.writeBoolean(false);
		//是否投降
		stream.writeBoolean(isSurrounded);
		stream.writeBoolean(false);
		stream.writeInt(-9999);
	}
	
	public void writePlayer(DataOutputStream stream, int ping) throws IOException{
		stream.writeByte(playerIndex);
		stream.writeInt(playerCredits);
		stream.writeInt(playerTeam);
		stream.writeBoolean(true);
		if(isAdmin){
			stream.writeUTF("[[[" + playerName + "]]]");
		}else{
			stream.writeUTF(playerName);
		}
		stream.writeBoolean(true);

		//enc.stream.writeBoolean(true);
		stream.writeInt(ping);
		stream.writeLong(System.currentTimeMillis());

		stream.writeBoolean(false);
		stream.writeInt(0);

		stream.writeInt(playerIndex);
		stream.writeByte(0);
		stream.writeBoolean(false);
		stream.writeBoolean(false);
		stream.writeBoolean(false);
		stream.writeBoolean(false);
		stream.writeInt(-9999);
	}

	public boolean movePlayer(int index){
		for(Player p : PlayerGroup.getPlayers()){
			try{
			if(p.playerIndex == index){
				return false;
			}
			}catch(NullPointerException e){continue;}
		}
		this.playerIndex = index;
		PlayerGroup.remove(this);
		PlayerGroup.set(index, this);
		return true;
	}

	public boolean moveTeam(int team){
		if(team > 9 && team < 0){
			return false;
		} else {
			this.playerTeam = team;
		}
		return true;
	}

	public boolean giveAdmin(int index){
		Player player = PlayerGroup.get(index);
		if(index < 9 && index >= 0 && player != null && this.isAdmin){
			player.isAdmin = true;
			this.isAdmin = false;
			return true;
		}
		return false;
	}
}

