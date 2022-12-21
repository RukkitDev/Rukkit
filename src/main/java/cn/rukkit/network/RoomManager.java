package cn.rukkit.network;

import cn.rukkit.config.RoundConfig;

import java.util.ArrayList;
import java.util.List;

public class RoomManager {
    public List<NetworkRoom> roomList;
    public RoomManager(RoundConfig defaultConfig, int maxRoom) {
        roomList = new ArrayList<NetworkRoom>(maxRoom);
    }

    public void addConnection(Connection conn, int roomId) {

    }

    public void addConnection(Connection conn) {

    }

    public NetworkRoom getDefaultRoom() {
        return roomList.get(0);
    }

    public NetworkRoom getRoom(int index) {
        return roomList.get(index);
    }

    public NetworkRoom getAvailableRoom() {
        for (NetworkRoom room: roomList) {
            if (room.playerManager.getPlayerCount() <= room.playerManager.getMaxPlayer()) continue;
            return room;
        }
        return null;
    }
}
