package io.rukkit.entity;
import java.lang.reflect.*;
import java.util.*;
import io.rukkit.*;
import io.rukkit.net.*;

public class PlayerGroup
{
	private static Player[] players = new Player[ServerProperties.maxPlayer + ServerProperties.maxWatcher];
	private static Player[] inGamePlayers = new Player[10];
	
	public static void add(Player p){
		for(int i=0;i<10;i++){
			try{
				players[i].isNull = false;continue;
				//if(players[i].isNull)continue;
			}catch(NullPointerException e){}
			p.playerIndex = i;
			players[i] = p;
			break;
		}
	}
	
	public static void addWithTeam(Player p){
		for(int i=0;i<10;i++){
			try{
				players[i].isNull = false;continue;
				//if(players[i].isNull)continue;
			}catch(NullPointerException e){}
			p.playerIndex = i;
			if(i % 2 == 1){
				p.playerTeam = 1;
			}
			players[i] = p;
			break;
			}
	}
	
	public static void remove(Player p){
		if(GameServer.isGaming()){
			p.ping = -1;
			return;
		}
		for(int i=0;i<10;i++){
			try{
			if(players[i] == p){
				players[i] = null;
			}
			}catch(NullPointerException e){
				continue;
			}
		}
	}
	
	public static void remove(int index){
		if(GameServer.isGaming()){
			players[index].ping = -1;
			return;
		}
		players[index] = null;
	}
	
	public static Player get(int index){
		try{
			return players[index];
		}catch(Exception e){
			return null;
		}
	}
	
	public static int getIndex(Player p){
		for(int i=0;i<10;i++){
			try{
			if(players[i] == p){
				return i;
			}
			}catch(NullPointerException e){
				continue;
			}
		}
		return -1;
	}
	
	public static Player getAdmin(){
		for(int i=0;i<10;i++){
			try{
				if(players[i].isAdmin){
					return players[i];
				}
			}catch(NullPointerException e){
				continue;
			}
		}
		return null;
	}
	
	public static int size(){
		int size = 0;
		for(int i=0;i<10;i++){
			try{
				players[i].toString();
				size++;
			}catch(NullPointerException e){
				continue;
			}
		}
		return size;
	}
	
	
	public static Player[] getPlayers(){
		return players;
	}
	
	public static void set(int index, Player p){
		players[index] =  p;
	}
	
	public static void reset(){
		players = new Player[10];
	}
}
