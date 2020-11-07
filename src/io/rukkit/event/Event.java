package io.rukkit.event;

public class Event
{
	//public boolean isCancellable = true;
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
