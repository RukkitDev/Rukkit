package cn.rukkit.network;

import cn.rukkit.Rukkit;
import cn.rukkit.config.RoundConfig;

import java.util.ArrayList;
import java.util.List;

public class RoomManager {
    public List<NetworkRoom> roomList;
    public RoomManager(RoundConfig defaultConfig, int maxRoom) {
        roomList = new ArrayList<NetworkRoom>(maxRoom);
        resetAllRooms();
    }

    public void addConnection(RoomConnection conn, int roomId) {

    }

    public void addConnection(RoomConnection conn) {

    }

    /**
     * 获取当前的默认房间(id = 0).
     * @return 房间实例
     */
    public NetworkRoom getDefaultRoom() {
        return roomList.get(0);
    }

    /**
     * 获取房间。根据房间id进入。
     * @param index
     * @return
     */
    public NetworkRoom getRoom(int index) {
        return roomList.get(index);
    }

    /**
     * 获得当前可用的房间。
     * @return NetworkRoom实例.
     */
    public NetworkRoom getAvailableRoom() {
        for (NetworkRoom room: roomList) {
            if (room.playerManager.getPlayerCount() < room.playerManager.getMaxPlayer()){
                if (!room.isGaming()) {
                    return room;
                }
            }
        }
        return null;
    }

    /**
     * Reset all Rooms.
     * 重置所有房间。重置后房间数是rukkit.yml处的 maxRoom
     */
    public void resetAllRooms() {
        for (NetworkRoom room: roomList) {
            if (room != null) {
                room.connectionManager.broadcastServerMessage("Room reset.");
                room.connectionManager.disconnect();
                room.discard();
                roomList.remove(room);
            }
        }
        for (int id = 0;id < Rukkit.getConfig().maxRoom;id ++){
            roomList.add(new NetworkRoom(id));
        }
    }
}
