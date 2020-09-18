package io.rukkit.plugin;

public abstract class RukkitPlugin
{
	public String pluginName;
	public String pluginVersion;
	public RukkitPlugin(){}
	public abstract void onStart()
	public abstract void onStop();
}
