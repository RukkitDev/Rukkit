package io.rukkit.event.game;

import io.rukkit.event.*;
import io.rukkit.event.game.*;

public class GameStopEvent extends GameEvent
{
	private static ListenerList list = new ListenerList(GameStopEvent.class);

	public static ListenerList getListenerList() {
		return list;
	}
}
