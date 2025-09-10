/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */
package cn.rukkit.network.room;

import cn.rukkit.Rukkit;
import cn.rukkit.config.RoundConfig;

import java.util.ArrayList;
import java.util.List;

public class ServerRoomManager {
    public List<ServerRoom> roomList;
    public ServerRoomManager(RoundConfig defaultConfig, int maxRoom) {
        roomList = new ArrayList<ServerRoom>(maxRoom);
        resetAllRooms();
    }

    public void addConnection(ServerRoomConnection conn, int roomId) {

    }

    public void addConnection(ServerRoomConnection conn) {

    }

    /**
     * 获取当前的默认房间(id = 0).
     * @return 房间实例
     */
    public ServerRoom getDefaultRoom() {
        return roomList.get(0);
    }

    /**
     * 获取房间。根据房间id进入。
     * @param index
     * @return
     */
    public ServerRoom getRoom(int index) {
        return roomList.get(index);
    }

    /**
     * 获得当前可用的房间。
     * @return NetworkRoom实例.
     */
    public ServerRoom getAvailableRoom() {
        for (ServerRoom room: roomList) {
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
        for (ServerRoom room: roomList) {
            if (room != null) {
                room.connectionManager.broadcastServerMessage("Room reset.");
                room.connectionManager.disconnect();
                room.discard();
                roomList.remove(room);
            }
        }
        for (int id = 0;id < Rukkit.getConfig().maxRoom;id ++){
            roomList.add(new ServerRoom(id));
        }
    }
}
