package io.rukkit.event.player;

import io.rukkit.entity.*;
import io.rukkit.event.*;
import io.rukkit.event.player.*;

public class PlayerLeftEvent extends PlayerEvent
{
	private static ListenerList list = new ListenerList(PlayerLeftEvent.class);

	public static ListenerList getListenerList() {
		return list;
	}

	public PlayerLeftEvent(Player p) {
		this.player = p;
	}

	private Player player;

	public Player getPlayer()
	{
		return player;
	}
}
