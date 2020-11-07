package io.rukkit.plugin;
import io.rukkit.util.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import io.rukkit.*;

public class PluginLoader
{

	private RukkitPlugin plugin;
	
	public PluginLoader(RukkitPlugin plugin, String name, String version,String main){
		plugin.pluginName = name;
		plugin.pluginVersion = version;
		plugin.mainClass = main;
		this.plugin = plugin;
	}
	
	public void load() {
		
	}
}
