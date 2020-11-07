package io.rukkit.event.player;
import io.rukkit.entity.*;
import io.rukkit.event.*;
import io.rukkit.event.action.*;

public class PlayerChatEvent extends PlayerEvent
{
	private static ListenerList list = new ListenerList(PlayerChatEvent.class);

	public static ListenerList getListenerList() {
		return list;
	}

	private Player player;
	private String message;
	
	public PlayerChatEvent(Player p, String message) {
		this.player = p;
		this.message = message;
	}
	
	public String getMessage() {
		return this.message;
	}
	
	public Player getPlayer() {
		return this.player;
	}
}
