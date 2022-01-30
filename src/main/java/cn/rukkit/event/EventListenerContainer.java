/*
 *  All Rights Reserved.
 *  FileName: EventListenerContainer.java
 *  @author: wtbdev
 *  @date: 2022/1/30 下午4:37
 */

package cn.rukkit.event;

import cn.rukkit.plugin.*;
import java.lang.reflect.*;
import java.util.*;

public class EventListenerContainer
{
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
			e.printStackTrace();
		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
		}
		catch (InvocationTargetException e)
		{
			e.printStackTrace();
		}
		return event;
	}


}
