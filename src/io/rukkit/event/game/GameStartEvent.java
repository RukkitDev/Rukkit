package io.rukkit.event.game;

import io.rukkit.event.*;
import io.rukkit.event.player.*;

public class GameStartEvent extends GameEvent
{
	private static ListenerList list = new ListenerList(GameStartEvent.class);

	public static ListenerList getListenerList() {
		return list;
	}
}
