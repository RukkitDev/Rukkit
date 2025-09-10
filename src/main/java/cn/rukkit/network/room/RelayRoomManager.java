package cn.rukkit.network.room;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RelayRoomManager {
    // 使用ConcurrentHashMap保证线程安全
    private static final Map<Integer, RelayNetworkRoom> roomMap = new ConcurrentHashMap<>();

    /**
     * 添加一个RelayNetworkRoom到管理器中
     * @param room 要添加的房间实例
     * @return 如果已存在相同ID的房间，则返回已存在的房间，否则返回null
     */
    public static RelayNetworkRoom addRelayRoom(RelayNetworkRoom room) {
        return roomMap.putIfAbsent(room.roomId, room);
    }

    /**
     * 根据roomId获取房间实例
     * @param roomId 房间ID
     * @return 对应的房间实例，如果不存在则返回null
     */
    public static RelayNetworkRoom getRoom(int roomId) {
        return roomMap.get(roomId);
    }

    /**
     * 根据roomId移除房间
     * @param roomId 要移除的房间ID
     * @return 被移除的房间实例，如果不存在则返回null
     */
    public static RelayNetworkRoom removeRoom(int roomId) {
        return roomMap.remove(roomId);
    }

    /**
     * 检查是否存在指定ID的房间
     * @param roomId 要检查的房间ID
     * @return 如果存在返回true，否则返回false
     */
    public static boolean containsRoom(int roomId) {
        return roomMap.containsKey(roomId);
    }

    /**
     * 获取当前管理的房间数量
     * @return 房间数量
     */
    public static int getRoomCount() {
        return roomMap.size();
    }

    /**
     * 清空所有房间
     */
    public static void clearAllRooms() {
        roomMap.clear();
    }
}