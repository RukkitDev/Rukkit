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

public class RoomStartGameEvent extends Event {
    private static ListenerList list = new ListenerList(RoomStartGameEvent.class);

    public static ListenerList getListenerList() {
        return list;
    }

    private NetworkRoom room;

    public NetworkRoom getRoom() {
        return room;
    }

    public RoomStartGameEvent(NetworkRoom room) {
        this.room = room;
    }
}
