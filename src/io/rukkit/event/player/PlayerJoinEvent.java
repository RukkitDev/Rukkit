package io.rukkit.event.player;
import io.rukkit.event.*;
import io.rukkit.entity.*;

public class PlayerJoinEvent extends PlayerEvent
{
	private static ListenerList list = new ListenerList(PlayerJoinEvent.class);
	
	public static ListenerList getListenerList() {
		return list;
	}
	
	public PlayerJoinEvent(Player p) {
		this.player = p;
	}
	
	private Player player;
	
	public Player getPlayer()
	{
		return player;
	}
}
