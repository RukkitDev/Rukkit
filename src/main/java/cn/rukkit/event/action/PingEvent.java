/*
 *  All Rights Reserved.
 *  FileName: PingEvent.java
 *  @author: wtbdev
 *  @date: 2022/1/30 下午4:37
 */

package cn.rukkit.event.action;

import cn.rukkit.event.*;
import cn.rukkit.game.*;

public class PingEvent extends ActionEvent implements Cancellable
{
	private static ListenerList list = new ListenerList(PingEvent.class);

	private String pingType;

	public static ListenerList getListenerList() {
		return list;
	}

	private NetworkPlayer player;
	private float targetX;
	private float targetY;

	public PingEvent(NetworkPlayer p, float x, float y, String pingType){
		this.player = p;
		this.targetX = x;
		this.targetY = y;
		this.pingType = pingType;
	}

	public float getTargetX()
	{
		return targetX;
	}

	public float getTargetY()
	{
		return targetY;
	}
	
	public NetworkPlayer getPlayer() {
		return player;
	}
	
	public String getPingType() {
		return pingType;
	}
}
