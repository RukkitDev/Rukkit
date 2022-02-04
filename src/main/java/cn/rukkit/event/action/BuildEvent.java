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
	private NetworkPlayer player;

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

	public NetworkPlayer getPlayer() {
		return this.player;
	}

	public BuildEvent (NetworkPlayer p, float targetX, float targetY, long fromUnitId, String targetUnitName) {
		this.player = p;
		this.targetX  = targetX;
		this.targetY  = targetY;
		this.fromUnitId  = fromUnitId;
		this.targetUnitName  = targetUnitName;
	}

	public BuildEvent (NetworkPlayer p, float targetX, float targetY, String targetUnitName) {
		this.player = p;
		this.targetX  = targetX;
		this.targetY  = targetY;
		this.fromUnitId  = 0;
		this.targetUnitName  = targetUnitName;
	}
}
