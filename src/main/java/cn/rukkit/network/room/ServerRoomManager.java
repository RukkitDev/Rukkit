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

/**
 * Master-compatible room registry for the migrated room model.
 *
 * <p>The registry is intentionally not installed into {@link Rukkit} yet.
 * The existing runtime still uses the legacy {@code RoomManager} until the
 * network entry point is migrated.</p>
 */
public class ServerRoomManager {
    public List<ServerRoom> roomList;

    public ServerRoomManager(RoundConfig defaultConfig, int maxRoom) {
        roomList = new ArrayList<>(maxRoom);
        resetAllRooms();
    }

    /**
     * Kept as a compatibility placeholder because the master implementation
     * does not register connections through the room manager yet.
     */
    public void addConnection(ServerRoomConnection connection, int roomId) {
    }

    /**
     * Kept as a compatibility placeholder because the master implementation
     * does not register connections through the room manager yet.
     */
    public void addConnection(ServerRoomConnection connection) {
    }

    public ServerRoom getDefaultRoom() {
        return roomList.get(0);
    }

    public ServerRoom getRoom(int index) {
        return roomList.get(index);
    }

    public ServerRoom getAvailableRoom() {
        for (ServerRoom room : roomList) {
            if (room.playerManager.getPlayerCount() < room.playerManager.getMaxPlayer()
                    && !room.isGaming()) {
                return room;
            }
        }
        return null;
    }

    /**
     * Reset all rooms while preserving the master lifecycle intent.
     *
     * <p>The legacy implementation removes entries from {@code roomList}
     * while iterating the same list, which fails as soon as the list contains
     * a room. The migrated registry performs the same broadcast/disconnect/
     * discard sequence on a snapshot and then rebuilds the list.</p>
     */
    public void resetAllRooms() {
        for (ServerRoom room : new ArrayList<>(roomList)) {
            if (room == null) {
                continue;
            }
            if (room.connectionManager != null) {
                room.connectionManager.broadcastServerMessage("Room reset.");
                room.connectionManager.disconnect();
            }
            if (room.playerManager != null && room.connectionManager != null) {
                room.discard();
            }
        }
        roomList.clear();
        for (int id = 0; id < Rukkit.getConfig().maxRoom; id++) {
            roomList.add(new ServerRoom(id));
        }
    }
}
