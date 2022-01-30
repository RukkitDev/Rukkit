/*
 *  All Rights Reserved.
 *  FileName: PlayerChatEvent.java
 *  @author: wtbdev
 *  @date: 2022/1/30 下午4:37
 */

package cn.rukkit.event.player;
import cn.rukkit.event.*;
import cn.rukkit.game.*;

public class PlayerChatEvent extends PlayerEvent implements Cancellable
{
	private static ListenerList list = new ListenerList(PlayerChatEvent.class);
	
	public static ListenerList getListenerList() {
		return list;
	}

	private NetworkPlayer player;
	private String message;

	public PlayerChatEvent(NetworkPlayer p, String message) {
		this.player = p;
		this.message = message;
	}

	public String getMessage() {
		return this.message;
	}

	public NetworkPlayer getPlayer() {
		return this.player;
	}
}
