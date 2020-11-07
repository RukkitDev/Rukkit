package io.rukkit.plugin;

/*
	When you developing a plugin, please extend RukkitPlugin not this class.
	注意:开发插件时请继承RukkitPlugin类.
*/
public interface Plugin
{
	public void onLoad()
	public void onEnable()
	public void onDisable()
	public void onServerDone()
	public void onStop()
}
