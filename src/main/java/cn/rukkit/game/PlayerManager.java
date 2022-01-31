/*
 *  All Rights Reserved.
 *  FileName: PlayerManager.java
 *  @author: wtbdev
 *  @date: 2022/1/30 下午4:37
 */

package cn.rukkit.game;
import cn.rukkit.*;

import java.util.Arrays;
//import sun.nio.ch.Net;

public class PlayerManager
{
	private int max;
	
	/**
	* Init player manager.
	* @params maxPlayer set up maxPlayer
	*/
	public PlayerManager(int maxPlayer) {
		this.max = maxPlayer;
		reset();
	}
	
	private NetworkPlayer[] players;
	//private static Player[] inGamePlayers = new Player[ServerProperties.maxPlayer];

	/**
	* Add a player into Array.
	*/
	public int add(NetworkPlayer p) {
		for(int i=0;i<players.length;i++){
			if (players[i].isEmpty) {
				p.playerIndex = i;
				players[i] = p;
				return i;
			}
		}
		return p.playerIndex;
	}
	
	/**
	* Add a player with auto team.
	* team changed by index.
	*/
	public void addWithTeam(NetworkPlayer p){
		if (add(p) % 2 == 1) {
			p.team = 1;
		}
	}

	/**
	 * Add a player with auto-team when no-stop mode.
	 *
	 */
	public void addWithTeamNoStop() {}

	/**
	* Remove a player.
	*/
	public void remove(NetworkPlayer p){
		int index = getIndex(p);
		remove(index);
	}

	/**
	* Remove player by index.
	*/
	public void remove(int index){
		if(Rukkit.getConfig().nonStopMode) {
			players[index] = new NetworkPlayer();
			return;
		}
		if(Rukkit.getGameServer().isGaming()){
			players[index].ping = -1;
			return;
		}
		players[index] = new NetworkPlayer();
	}
	
	/**
	* Get player by index.
	*/
	public NetworkPlayer get(int index){
		if (index > players.length - 1) return null;
		return players[index];
	}

	public NetworkPlayer getPlayerByUUID(String uuid) {
		for (NetworkPlayer p: players) {
			if (p.uuid.equals(uuid)) {
				return p;
			}
		}
		return null;
	}
	
	/**
	* get a player index.
	*/
	public int getIndex(NetworkPlayer p){
		for(int i=0;i<players.length;i++){
			if(players[i] == p) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	* get admin player.
	*/
	public NetworkPlayer getAdmin(){
		for (NetworkPlayer p: players) {
			if (p.isAdmin && !p.isEmpty) {
				return p;
			}
		}
		return null;
	}
	
	/**
	* get player amount.
	*/
	public int getPlayerCount(){
		int size = 0;
		for (NetworkPlayer p: players) {
			if (!p.isEmpty) {
				size++;
			}
		}
		return size;
	}

	// ai方法
	public void addAI() {
		NetworkPlayer p = new NetworkPlayer();
		p.isEmpty = false;
		p.isAI = true;
		p.name = "AI - Idiot";
		p.ping = -1;
		add(p);
	}

	public void removeAI() {

	}
	
	/**
	* returns a player array INCLUDING null.
	* @return NetworkPlayer[] array
	*/
	public NetworkPlayer[] getPlayerArray(){
		return players;
	}
	
	/**
	* set player in a index
	*/
	public void set(int index, NetworkPlayer p){
		players[index] =  p;
	}
	
	/**
	* reset array.useful for reseting a game.
	*/
	public void reset(){
		players = new NetworkPlayer[max];
		for (int i = 0;i < players.length;i++) {
			NetworkPlayer emptyPlayer = new NetworkPlayer();
			emptyPlayer.playerIndex = i;
			if (i % 2 == 1) emptyPlayer.team = 1;
			players[i] = emptyPlayer;
		}
	}
}
