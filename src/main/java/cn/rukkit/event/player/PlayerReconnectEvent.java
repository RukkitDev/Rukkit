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
