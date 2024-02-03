/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.event;

import cn.rukkit.event.*;
import java.util.*;
import cn.rukkit.util.*;
import org.slf4j.*;
import cn.rukkit.plugin.RukkitPlugin;

public class ListenerList
{
	private Class eventClass;
	private static Logger log;
	private ArrayList<EventListenerContainer> listenerList = new ArrayList<EventListenerContainer>();

	public ListenerList(Class clazz) {
		this.eventClass = clazz;
		this.log = LoggerFactory.getLogger("List" + eventClass.toString());
	}

	public void registerListener(EventListenerContainer listener) {
		this.listenerList.add(listener);
	}

	public boolean callListeners(Event event) {
		for(EventListenerContainer listener : listenerList) {
			Event e = listener.callMethod(event);
			log.debug("isCanceled: " + e.isCancelled);
			if (e.isCancelled) {
				return false;
			}
		}
		return true;
	}
	
	public void removePluginListener(EventListener listener) {
		// 更安全的移除
		Iterator<EventListenerContainer> iterator = listenerList.iterator();
		while (iterator.hasNext()) {
			EventListenerContainer container = iterator.next();
			if (container.listener.getClass() == listener.getClass()) {
				iterator.remove();
			}
		}
		/*
		for (EventListenerContainer c: listenerList) {
			if (c.listener.getClass() == listener.getClass()) {
				listenerList.remove(c);
			}
		}*/
	}

	public void clear() {
		this.listenerList.clear();
	}
}

