/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 */

package cn.rukkit.event.player;

import cn.rukkit.event.ListenerList;
import cn.rukkit.game.NetworkPlayer;

public class PlayerReconnectEvent extends PlayerEvent{
    private static ListenerList list = new ListenerList(PlayerReconnectEvent.class);

    public static ListenerList getListenerList() {
        return list;
    }

    public PlayerReconnectEvent(NetworkPlayer p) {
        this.player = p;
    }

    private NetworkPlayer player;

    public NetworkPlayer getPlayer()
    {
        return player;
    }
}
