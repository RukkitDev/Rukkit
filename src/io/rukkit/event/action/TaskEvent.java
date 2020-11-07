package io.rukkit.event.action;
import io.rukkit.entity.*;
import io.rukkit.event.*;

public class TaskEvent extends ActionEvent implements Cancellable
{
	private static ListenerList list = new ListenerList(TaskEvent.class);

	public static ListenerList getListenerList() {
		return list;
	}

	private Player player;
	private String taskString;
	private long actionUnitId;

	public TaskEvent(Player p, String task, long id){
		this.player = p;
		this.taskString = task;
		this.actionUnitId = id;
	}

	public TaskEvent(Player p, String task) {
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

	public Player getPlayer() {
		return this.player;
	}
}
