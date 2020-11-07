package io.rukkit.plugin;

public class PluginContainer
{
	public String pluginName;
	public String pluginVersion;
	public RukkitPlugin plugin;
	public PluginContainer(String name, String version, RukkitPlugin plugin){
		this.pluginName = name;
		this.pluginVersion = version;
		this.plugin = plugin;
		plugin.pluginName = name;
		plugin.pluginVersion = version;
	}
}
