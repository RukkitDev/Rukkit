package cn.rukkit.event;

import cn.rukkit.event.*;
import java.util.*;
import cn.rukkit.util.*;
import org.slf4j.*;

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

	public void clear() {
		this.listenerList.clear();
	}
}

