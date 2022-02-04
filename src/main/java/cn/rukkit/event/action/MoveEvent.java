/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

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
