package io.rukkit.event.action;
import io.rukkit.entity.*;
import io.rukkit.event.*;

public class MoveEvent extends ActionEvent implements Cancellable
{
	private static ListenerList list = new ListenerList(MoveEvent.class);

	public static ListenerList getListenerList() {
		return list;
	}
	
	private Player player;
	private float targetX;
	private float targetY;
	private long actionUnitId;
	
	public MoveEvent(Player p, float x, float y, long id){
		this.player = p;
		this.targetX = x;
		this.targetY = y;
		this.actionUnitId = id;
	}
	
	public MoveEvent(Player p, float x, float y) {
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

	public Player getPlayer() {
		return this.player;
	}
}
