/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.game;
//import sun.nio.ch.Net;

public class GlobalPlayerManager
{
    private int max;

    /**
     * Init player manager.
     * @params maxPlayer set up maxPlayer
     */
    public GlobalPlayerManager() {
    }

    private NetworkPlayer[] players;
    //private static Player[] inGamePlayers = new Player[ServerProperties.maxPlayer];



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

    public void removeAI() {

    }

    /**
     * returns a player array INCLUDING null.
     * @return @nullable NetworkPlayer[] array
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

    public int getMaxPlayer() {
        return max;
    }
}
