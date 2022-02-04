/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.event.player;
import cn.rukkit.event.*;
import cn.rukkit.game.*;

public class PlayerChatEvent extends PlayerEvent implements Cancellable
{
	private static ListenerList list = new ListenerList(PlayerChatEvent.class);
	
	public static ListenerList getListenerList() {
		return list;
	}

	private NetworkPlayer player;
	private String message;

	public PlayerChatEvent(NetworkPlayer p, String message) {
		this.player = p;
		this.message = message;
	}

	public String getMessage() {
		return this.message;
	}

	public NetworkPlayer getPlayer() {
		return this.player;
	}
}
