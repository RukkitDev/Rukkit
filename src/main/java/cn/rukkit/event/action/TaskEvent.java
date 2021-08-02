package cn.rukkit.event.action;

import cn.rukkit.event.*;
import cn.rukkit.event.action.*;
import cn.rukkit.game.*;

public class TaskEvent extends ActionEvent implements Cancellable 
{
	private static ListenerList list = new ListenerList(TaskEvent.class);

	public static ListenerList getListenerList() {
		return list;
	}

	private NetworkPlayer player;
	private String taskString;
	private long actionUnitId;
	private boolean isCancel = false;
	
	public TaskEvent(NetworkPlayer p, String task, long id, boolean isCancel) {
		this.player = p;
		this.taskString = task;
		this.actionUnitId = id;
		this.isCancel = isCancel;
	}

	public TaskEvent(NetworkPlayer p, String task, long id){
		this.player = p;
		this.taskString = task;
		this.actionUnitId = id;
	}

	public TaskEvent(NetworkPlayer p, String task) {
		this.player = p;
		this.taskString = task;
		this.actionUnitId = 0;
	}

	public String getTaskString() {
		return this.taskString;
	}

	public long getActionUnitId() {
		return this.actionUnitId;
	}

	public NetworkPlayer getPlayer() {
		return this.player;
	}
	
	public boolean isCancel() {
		return isCancel;
	}
}

