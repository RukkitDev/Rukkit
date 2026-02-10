/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.event;

import cn.rukkit.plugin.*;
import java.lang.reflect.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventListenerContainer
{
	private static final Logger log = LoggerFactory.getLogger(EventListenerContainer.class);
	public Method method;
	public RukkitPlugin plugin;
	public EventListener listener;
	public EventListenerContainer(RukkitPlugin plugin, Method method, EventListener listener){
		this.method = method;
		this.listener = listener;
		this.plugin = plugin;
	}

	public Event callMethod(Event event) {
		method.setAccessible(true);
		try
		{
			method.invoke(listener, event);
			//listener.invoke(new PlayerJoinEvent(), new PlayerJoinEvent());
		}
		catch (IllegalAccessException e)
		{
			log.error("Illegal access in event method invocation", e);
		}
		catch (IllegalArgumentException e)
		{
			log.error("Illegal argument in event method invocation", e);
		}
		catch (InvocationTargetException e)
		{
			log.error("Invocation target error in event method", e);
		}
		return event;
	}


}
