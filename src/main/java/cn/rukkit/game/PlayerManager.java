package cn.rukkit.game;
import cn.rukkit.*;

public class PlayerManager
{
	private int max;
	
	/**
	* Init player manager.
	* @params maxPlayer set up maxPlayer
	*/
	public PlayerManager(int maxPlayer) {
		players = new NetworkPlayer[maxPlayer];
		max = maxPlayer;
	}
	
	private NetworkPlayer[] players;
	//private static Player[] inGamePlayers = new Player[ServerProperties.maxPlayer];

	/**
	* Add a player into Array.
	*/
	public void add(NetworkPlayer p){
		for(int i=0;i<players.length;i++){
			try{
				players[i].isNull();continue;
				//if(players[i].isNull)continue;
			}catch(NullPointerException e){}
			p.playerIndex = i;
			players[i] = p;
			break;
		}
	}
	
	/**
	* Add a player with auto team.
	* team changed by index.
	*/
	public void addWithTeam(NetworkPlayer p){
		for(int i=0;i<players.length;i++){
			try{
				players[i].isNull();continue;
				//if(players[i].isNull)continue;
			}catch(NullPointerException e){}
			p.playerIndex = i;
			if(i % 2 == 1){
				p.team = 1;
			}
			players[i] = p;
			break;
		}
	}

	/**
	* Remove a player.
	*/
	public void remove(NetworkPlayer p){
		if(Rukkit.getGameServer().isGaming()){
			p.ping = -1;
			return;
		}
		for(int i=0;i<players.length;i++){
			try{
				if(players[i] == p){
					players[i] = null;
				}
			}catch(NullPointerException e){
				continue;
			}
		}
	}

	/**
	* Remove player by index.
	*/
	public void remove(int index){
		if(Rukkit.getGameServer().isGaming()){
			players[index].ping = -1;
			return;
		}
		players[index] = null;
	}
	
	/**
	* Get player by index.
	*/
	public NetworkPlayer get(int index){
		try{
			return players[index];
		}catch(Exception e){
			return null;
		}
	}
	
	/**
	* get a player index.
	*/
	public int getIndex(NetworkPlayer p){
		for(int i=0;i<players.length;i++){
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
	
	/**
	* get admin player.
	*/
	public NetworkPlayer getAdmin(){
		for(int i=0;i<players.length;i++){
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
	
	/**
	* get player amount.
	*/
	public int getPlayerCount(){
		int size = 0;
		for(int i=0;i<players.length;i++){
			try{
				players[i].isNull();
				size++;
			}catch(NullPointerException e){
				continue;
			}
		}
		return size;
	}

	// ai方法
	public void addAI() {
		NetworkPlayer p = new NetworkPlayer(null);
		p.isAI = true;
		p.name = "AI - ?";
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
	}
}
