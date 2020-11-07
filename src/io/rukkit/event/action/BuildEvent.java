package io.rukkit.event.action;
import io.rukkit.event.*;
import io.rukkit.entity.*;

public class BuildEvent extends ActionEvent implements Cancellable
{
	private static ListenerList list = new ListenerList(BuildEvent.class);

	public static ListenerList getListenerList() {
		return list;
	}
	
	private float targetX;

	private float targetY;

	private String targetUnitName;

	private long fromUnitId;
	private Player player;
	
	public float getTargetX()
	{
		return targetX;
	}

	public float getTargetY()
	{
		return targetY;
	}

	public String getTargetUnitName()
	{
		return targetUnitName;
	}

	public long getFromUnitId()
	{
		return fromUnitId;
	}
	
	public Player getPlayer() {
		return this.player;
	}
	
	public BuildEvent (Player p, float targetX, float targetY, long fromUnitId, String targetUnitName) {
		this.player = p;
		this.targetX  = targetX;
		this.targetY  = targetY;
		this.fromUnitId  = fromUnitId;
		this.targetUnitName  = targetUnitName;
	}
	
	public BuildEvent (Player p, float targetX, float targetY, String targetUnitName) {
		this.player = p;
		this.targetX  = targetX;
		this.targetY  = targetY;
		this.fromUnitId  = 0;
		this.targetUnitName  = targetUnitName;
	}
}
