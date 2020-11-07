package io.rukkit.plugin;
import io.rukkit.entity.*;
import io.rukkit.plugin.*;
import io.rukkit.plugin.listener.*;
import io.rukkit.util.*;
import java.util.*;
import io.rukkit.game.*;
import io.rukkit.command.*;

public class PluginHandler
{
	private Logger log = new Logger("PluginHandler");
	ArrayList<PluginContainer> plugins = new ArrayList<PluginContainer>();
	ArrayList<BaseListener> listeners = new ArrayList<BaseListener>();
	
	public void registerListener(BaseListener listener){
		listeners.add(listener);
		log.d("Listener added. count=" + listeners.size());
	}
	
	public void onPlayerJoined(Player p){
		for(BaseListener l: listeners){
			if (l instanceof PlayerJoinedListener){
				((PlayerJoinedListener)l).onJoined(p);
			}
		}
	}
	
	public void onChatRecieve(Player p, String msg)
	{
		for(BaseListener l: listeners){
			if (l instanceof MessageListener){
				((MessageListener)l).onRecieve(p, msg);
			}
		}
	}
	
	public boolean onAction(Player p, GameAction action){
		boolean bool = true;
		for(BaseListener l: listeners){
			if (l instanceof ActionListener){
				switch (action.getType()) {
					case BUILD:
						if(l instanceof BuildActionListener && !((BuildActionListener) l).onBuild(p, (BuildAction)action)){
							bool = false;
						}
						break;
					case PRODUCE:
						if(l instanceof ProduceActionListener &&!((ProduceActionListener) l).onProduce(p, (ProduceAction)action)){
							bool = false;
						}
						break;
					}
				}
			}
		return bool;
	}
	
	public void onPlayerLeft(Player p){
		for(BaseListener l: listeners){
			if (l instanceof PlayerLeftListener){
				((PlayerLeftListener)l).onLeft(p);
			}
		}
	}
}
