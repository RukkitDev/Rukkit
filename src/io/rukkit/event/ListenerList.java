package io.rukkit.event;
import io.rukkit.event.EventListener;
import java.util.*;
import io.rukkit.util.*;

public class ListenerList
{
	private Class eventClass;
	private static Logger log;
	private ArrayList<EventListenerContainer> listenerList = new ArrayList<EventListenerContainer>();
	
	public ListenerList(Class clazz) {
		this.eventClass = clazz;
		this.log = new Logger("List" + eventClass.toString());
	}
	
	public void registerListener(EventListenerContainer listener) {
		this.listenerList.add(listener);
	}
	
	public boolean callListeners(Event event) {
		for(EventListenerContainer listener : listenerList) {
			Event e = listener.callMethod(event);
			log.d("isCanceled: " + e.isCancelled);
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
