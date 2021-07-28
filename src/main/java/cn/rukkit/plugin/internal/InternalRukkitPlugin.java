package cn.rukkit.plugin.internal;
import cn.rukkit.plugin.*;

abstract class InternalRukkitPlugin extends RukkitPlugin
{
	public InternalRukkitPlugin() {
		loadConfig();
	}
	
	public abstract void loadConfig();
}
