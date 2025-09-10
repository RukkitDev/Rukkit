/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 * 
 * 基于 Rukkit 项目的衍生作品
 * Derived work based on Rukkit project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
/*
 * Copyright 2025 Micro(MCLDY@outlook.com) and contributors.
 * 
 * 本衍生作品基于 AGPLv3 许可证
 * This derivative work is licensed under AGPLv3
 */

package cn.rukkit.event.action;

import cn.rukkit.event.*;
import cn.rukkit.game.*;
import java.util.List;

public class MoveEvent extends ActionEvent implements Cancellable {
	private static ListenerList list = new ListenerList(MoveEvent.class);

	public static ListenerList getListenerList() {
		return list;
	}

	private NetworkPlayer player;
	private float targetX;
	private float targetY;
	private List<Long> actionUnitIds;

	public MoveEvent(NetworkPlayer p, float x, float y, List<Long> unitIds) {
		this.player = p;
		this.targetX = x;
		this.targetY = y;
		this.actionUnitIds = unitIds;
	}

	public float getTargetX() {
		return targetX;
	}

	public float getTargetY() {
		return targetY;
	}

	public List<Long> getActionUnitIds() {
		return this.actionUnitIds;
	}

	public int getUnitCount() {
		return this.actionUnitIds.size();
	}

	public NetworkPlayer getPlayer() {
		return this.player;
	}
}