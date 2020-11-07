package io.rukkit.plugin;

import io.rukkit.*;
import io.rukkit.event.*;
import io.rukkit.event.EventListener;
import io.rukkit.util.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.lang.reflect.*;
import io.rukkit.event.player.*;
import io.rukkit.net.*;

public final class PluginManager
{
	private static final String PLUGIN_FOLDER= "plugins";
	private static Logger log = new Logger("PluginManager");
	private static boolean isServerDone = false;
	private static List<RukkitPlugin> pluginList = new ArrayList<RukkitPlugin>();
	//加载plugins文件夹内的插件
	public void loadPluginInDir() throws IOException
	{
		log.i("Plugin loader started");
		File folder = new File(PLUGIN_FOLDER);
		folder.mkdir();
		if (!folder.isDirectory())
		{
			log.w("Not a dir.Change a another dir...");
			folder = new File(PLUGIN_FOLDER + "_rukkit");
			folder.mkdir();
			if (!folder.exists() || !folder.isDirectory())
			{
				log.e("Load failed.Stop working.");
				return;
			}
		}

		for (File f: folder.listFiles())
		{
			String name = f.getName();
			String[] names = name.split("\\.");
			//log.d(f.getName()+names.toString());
			if (names[names.length - 1].equals("jar"))
			{
				JarFile jar = new JarFile(f.getPath());
				Enumeration<JarEntry> e = jar.entries();
				while (e.hasMoreElements())
				{
					JarEntry entry = e.nextElement();
					if (entry.getName().contains("rukkitmeta.properties"))
					{
						//System.out.println("go");
						Properties per = new Properties();
						per.load(jar.getInputStream(entry));
						String pluginName = per.getProperty("plugin-name", "null");
						String pluginVersion = per.getProperty("plugin-version", "null");
						String executePath = per.getProperty("main-class", "null");
						if (pluginName.equals("null") || pluginVersion.equals("null"))
						{
							log.e("Bad plugin file.Plugin name/version not found in properties.");
							log.e("File: " + f);
						}
						log.i("Loading plugin: " + pluginName + " v" + pluginVersion);
						if (executePath.equals("null"))
						{
							log.e("Plugin " + pluginName + " not have a Main class.Please check your properties file.");
						}
						log.d(executePath);
						URL url1 = new URL("file:" + f.getAbsolutePath());
						URLClassLoader myClassLoader1 = new URLClassLoader(new URL[] { url1 }, Thread.currentThread().getContextClassLoader());
						try
						{
							Class<?> pluginClass = myClassLoader1.loadClass(executePath);
							RukkitPlugin plugin = (RukkitPlugin) pluginClass.newInstance();
							//plugin.onLoad();
							PluginLoader loader = new PluginLoader(plugin, pluginName, pluginVersion, executePath);
							plugin.initPlugin(loader);
						}
						catch (IllegalAccessException e1)
						{
							log.e("Plugin could not be loaded.");
							e1.getStackTrace();
						}
						catch (InstantiationException e2)
						{
							log.e("Plugin could not be loaded.");
							e2.getStackTrace();
						}
						catch (ClassNotFoundException e3)
						{
							log.e("Plugin could not be loaded.Class not found.");
							e3.printStackTrace();
						}
						enableAllPlugins();
					}
				}
			}
		}
		log.i("Load complete!");
	}
	
	public void serverDone(GameServer g) {
		if (isServerDone) {
			throw new RuntimeException("Server already started.");
		} else {
			isServerDone = true;
			for(RukkitPlugin p : pluginList) {
				p.onServerDone();
			}
		}
	}

	//加载单个插件类
	public void loadPlugin(RukkitPlugin plugin)
	{
		try
		{
			if (pluginList.get(pluginList.indexOf(plugin)) == null)
			{
				plugin.onLoad();
				pluginList.add(plugin);
			}
			else
			{
				log.w("Plugin " + plugin.getPluginName() + " had already added.");
			}
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			plugin.onLoad();
			pluginList.add(plugin);
		}
	}

	private void enablePluginDirectly(RukkitPlugin plugin)
	{
		plugin.setEnabled(true);
	}

	private void disablePluginDirectly(RukkitPlugin plugin)
	{
		plugin.setEnabled(false);
	}

	//启用已加载的插件
	public void enablePlugin(RukkitPlugin plugin)
	{
		try
		{
			pluginList.get(pluginList.indexOf(plugin)).setEnabled(true);
		}
		catch (Exception e)
		{
			log.e("A exception occured. :" + e);
		}
	}

	//禁用已加载的插件
	public void disablePlugin(RukkitPlugin plugin)
	{
		try
		{
			pluginList.get(pluginList.indexOf(plugin)).setEnabled(false);
		}
		catch (Exception e)
		{
			log.e("A exception occured. :" + e);
		}
	}

	//禁用所有插件
	void disableAllPlugins()
	{
		for (RukkitPlugin plugin: pluginList)
		{
			disablePluginDirectly(plugin);
		}
	}

	//启用所有插件
	void enableAllPlugins()
	{
		for (RukkitPlugin plugin: pluginList)
		{
			enablePluginDirectly(plugin);
		}
	}

	public void registerEventListener(EventListener listener, RukkitPlugin plugin)
	{
		//获取公开方法
		Method methods[] = listener.getClass().getMethods();
		log.d(" ");
		for (Method method: methods)
		{
			log.d("get");
			//判断方法是否进行注解
			if (method.isAnnotationPresent(EventHandler.class) && !method.isSynthetic())
			{
				//log.d("isAnno");
				//判断方法第一个参数是否为Event
				if (method.getParameterCount() != 0 && Event.class.isAssignableFrom(method.getParameterTypes()[0]))
				{
					method.setAccessible(true);
					log.d("Got Event was: " + method.getParameters()[0].getType());
					try
					{
						log.d("got called");
						ListenerList list = ((ListenerList) method.getParameters()[0].getType().getMethod("getListenerList").invoke(null));
						list.registerListener(new EventListenerContainer(plugin, method, listener));
						//method.invoke(listener, new PlayerJoinEvent());
						//list.callListeners(new PlayerJoinEvent());
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			}
			else
			{
				continue;
			}
		}
	}

	//Unfinished
	public void unregisterEventListener(EventListener listener)
	{
		Method methods[] = listener.getClass().getMethods();
		log.d(" ");
		for (Method method: methods)
		{
			log.d("get");
			//判断方法是否进行注解
			if (method.isAnnotationPresent(EventHandler.class) && !method.isSynthetic())
			{
				//log.d("isAnno");
				//判断方法第一个参数是否为Event
				if (method.getParameterCount() != 0 && Event.class.isAssignableFrom(method.getParameterTypes()[0]))
				{
					method.setAccessible(true);
					log.d("Got Event was: " + method.getParameters()[0].getType());
					try
					{
						log.d("got called");
						ListenerList list = ((ListenerList) method.getParameters()[0].getType().getMethod("getListenerList").invoke(null));
						//list.registerListener(new EventListenerContainer(plugin, method, listener));
						//method.invoke(listener, new PlayerJoinEvent());
						//list.callListeners(new PlayerJoinEvent());
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			}
			else
			{
				continue;
			}
		}
	}
}
