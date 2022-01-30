/*
 *  All Rights Reserved.
 *  FileName: PlayerJoinEvent.java
 *  @author: wtbdev
 *  @date: 2022/1/30 下午4:37
 */

package cn.rukkit.event.player;

import cn.rukkit.event.*;
import cn.rukkit.event.player.*;
import cn.rukkit.game.*;

public class PlayerJoinEvent extends PlayerEvent 
{
	private static ListenerList list = new ListenerList(PlayerJoinEvent.class);

	public static ListenerList getListenerList() {
		return list;
	}

	public PlayerJoinEvent(NetworkPlayer p) {
		this.player = p;
	}

	private NetworkPlayer player;

	public NetworkPlayer getPlayer()
	{
		return player;
	}
}
