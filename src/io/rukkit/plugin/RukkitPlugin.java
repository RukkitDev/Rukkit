package io.rukkit.plugin;
import io.rukkit.*;
import io.rukkit.net.*;
import io.rukkit.util.*;

public abstract class RukkitPlugin implements Plugin
{
	String pluginName;
	String pluginVersion;
	String mainClass;
	boolean isEnabled;
	private Logger log;
	
	public RukkitPlugin(){}
	public PluginManager getPluginManager(){
		return Rukkit.getCurrentPluginManager();
		//return Rukkit.getCurrentPluginHandler();
	}
	
	public final void initPlugin(PluginLoader loader) {
		getPluginManager().loadPlugin(this);
		log = new Logger(this.getClass().toString());
	}
	
	public final void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
		if (isEnabled) {
			onEnable();
		} else {
			onDisable();
		}
	}
	
	public final boolean getEnabled() {
		return isEnabled;
	}
	
	public final String getPluginName() {
		return pluginName;
	}
	
	public final String getPluginVersion() {
		return pluginVersion;
	}
	
	public final String getMainClass() {
		return mainClass;
	}
	
	public final Logger getLogger() {
		return log;
	}
	
	
	public abstract void onLoad();
	public abstract void onEnable();
	public abstract void onDisable();
	public abstract void onServerDone();
	public abstract void onStop();
}
