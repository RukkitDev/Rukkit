/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.event;

public class Event
{
	public boolean isCancelled = false;

	public void setCancel(boolean cancel) {
		if (this instanceof Cancellable) {
			this.isCancelled = cancel;
		} else {
			//throw new RuntimeException("Event not cancellable");
			//Do nothing.
		}
	}

	public boolean isCancelled() {
		if (this instanceof Cancellable) {
			return isCancelled;
		} else {
			//throw new RuntimeException("Event not cancellable");
			return false;
		}
	}
}
