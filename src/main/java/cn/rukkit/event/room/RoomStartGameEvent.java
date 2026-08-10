/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.event.room;

import cn.rukkit.event.Event;
import cn.rukkit.event.ListenerList;
import cn.rukkit.network.NetworkRoom;
import cn.rukkit.network.room.ServerRoom;

public class RoomStartGameEvent extends Event {
    private static ListenerList list = new ListenerList(RoomStartGameEvent.class);

    public static ListenerList getListenerList() {
        return list;
    }

    private NetworkRoom room;
    private ServerRoom serverRoom;

    public NetworkRoom getRoom() {
        return room;
    }

    public ServerRoom getServerRoom() {
        return serverRoom;
    }

    public RoomStartGameEvent(NetworkRoom room) {
        this.room = room;
    }

    public RoomStartGameEvent(ServerRoom room) {
        this.serverRoom = room;
    }
}
