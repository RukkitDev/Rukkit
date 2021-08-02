package cn.rukkit.event.action;

import cn.rukkit.event.*;
import cn.rukkit.event.action.*;
import cn.rukkit.game.*;

public class MoveEvent extends ActionEvent implements Cancellable
{
	private static ListenerList list = new ListenerList(MoveEvent.class);

	public static ListenerList getListenerList() {
		return list;
	}

	private NetworkPlayer player;
	private float targetX;
	private float targetY;
	private long actionUnitId;

	public MoveEvent(NetworkPlayer p, float x, float y, long id){
		this.player = p;
		this.targetX = x;
		this.targetY = y;
		this.actionUnitId = id;
	}

	public MoveEvent(NetworkPlayer p, float x, float y) {
		this.player = p;
		this.targetX = x;
		this.targetY = y;
		this.actionUnitId = 0;
	}

	public float getTargetX()
	{
		return targetX;
	}

	public float getTargetY()
	{
		return targetY;
	}

	public long getActionUnitId() {
		return this.actionUnitId;
	}

	public NetworkPlayer getPlayer() {
		return this.player;
	}
}
