/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.plugin;

import cn.rukkit.*;
import cn.rukkit.event.*;
import cn.rukkit.event.EventListener;
import cn.rukkit.plugin.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import org.slf4j.*;
import cn.rukkit.network.*;
import org.yaml.snakeyaml.*;

public class PluginManager 
{
	private static String PLUGIN_FOLDER= Rukkit.getEnvPath() + "/plugins";
	private static Logger log = LoggerFactory.getLogger(PluginManager.class);
	private static boolean isServerDone = false;
	private static List<RukkitPlugin> pluginList = new ArrayList<RukkitPlugin>();
	//加载plugins文件夹内的插件
	public void loadPluginInDir() throws IOException
	{
		log.info("Plugin loader started");
		File folder = new File(PLUGIN_FOLDER);
		folder.mkdir();
		if (!folder.isDirectory())
		{
			log.warn("Not a dir.Change a another dir...");
			folder = new File(PLUGIN_FOLDER + "_rukkit");
			folder.mkdir();
			if (!folder.exists() || !folder.isDirectory())
			{
				log.error("Load failed.Stop working.");
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
					if (entry.getName().contains("plugin.yml"))
					{
						//System.out.println("go");
						PluginConfig pluginConf = new Yaml().loadAs(jar.getInputStream(entry), PluginConfig.class);
						//String executePath = per.getProperty("main-class", "null");
						if (pluginConf.name.equals("null") || pluginConf.version.equals("null"))
						{
							log.error("Bad plugin file.Plugin name/version not found in properties.");
							log.error("File: " + f);
						}
						if (pluginConf.pluginClass == null)
						{
							log.error("Plugin " + pluginConf.name + " not have a Main class.Please check your properties file.");
						}
						log.debug(pluginConf.pluginClass);
						URL url1 = new URL("file:" + f.getAbsolutePath());
						URLClassLoader myClassLoader1 = new URLClassLoader(new URL[] { url1 }, Thread.currentThread().getContextClassLoader());
						try
						{
							Class<?> pluginClass = myClassLoader1.loadClass(pluginConf.pluginClass);
							RukkitPlugin plugin = (RukkitPlugin) pluginClass.newInstance();
							plugin.config = pluginConf;
							loadPlugin(plugin);
							//plugin.onLoad();
							/*PluginLoader loader = new PluginLoader(plugin, pluginName, pluginVersion, executePath);
							plugin.initPlugin(loader);*/
						}
						catch (IllegalAccessException e1)
						{
							log.error("Plugin could not be loaded.",e1);
							//e1.getStackTrace();
						}
						catch (InstantiationException e2)
						{
							log.error("Plugin could not be loaded.", e2);
							//e2.getStackTrace();
						}
						catch (ClassNotFoundException e3)
						{
							log.error("Plugin could not be loaded.Class not found.", e3);
							//e3.printStackTrace();
						}
						enableAllPlugins();
					}
				}
			}
		}
		log.info("Load complete!");
	}

	public void serverDone(RoomGameServer g) {
		if (isServerDone) {
			throw new RuntimeException("Server already started.");
		} else {
			isServerDone = true;
			for(RukkitPlugin p : pluginList) {
				p.onDone();
			}
		}
	}

	//加载单个插件类
	public void loadPlugin(RukkitPlugin plugin)
	{
		log.info("Loading plugin: " + plugin.config.name + " v" + plugin.config.version);
		try
		{
			if (pluginList.get(pluginList.indexOf(plugin)) == null)
			{
				plugin.onLoad();
				pluginList.add(plugin);
			}
			else
			{
				log.warn("Plugin " + plugin.config.name + " had already added.");
			}
		}
		catch (IndexOutOfBoundsException e)
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
			log.error("A exception occured. :" ,e);
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
			log.error("A exception occured. :" , e);
		}
	}

	//禁用所有插件
	void disableAllPlugins()
	{
		for (RukkitPlugin plugin: pluginList)
		{
			disablePlugin(plugin);
		}
	}

	//启用所有插件
	void enableAllPlugins()
	{
		for (RukkitPlugin plugin: pluginList)
		{
			enablePlugin(plugin);
		}
	}

	public void registerEventListener(EventListener listener, RukkitPlugin plugin)
	{
		//获取公开方法
		Method methods[] = listener.getClass().getMethods();
		log.debug(" ");
		for (Method method: methods)
		{
			log.debug("get:" + method.getName());
			//判断方法是否进行注解
			if (method.isAnnotationPresent(EventHandler.class) && !method.isSynthetic())
			{
				//log.d("isAnno");
				//判断方法第一个参数是否为Event
				if (method.getParameterCount() != 0 && Event.class.isAssignableFrom(method.getParameterTypes()[0]))
				{
					method.setAccessible(true);
					log.debug("Got Event was: " + method.getParameters()[0].getType());
					try
					{
						log.debug("got called");
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
		log.debug(" ");
		for (Method method: methods)
		{
			log.debug("get");
			//判断方法是否进行注解
			if (method.isAnnotationPresent(EventHandler.class) && !method.isSynthetic())
			{
				//log.d("isAnno");
				//判断方法第一个参数是否为Event
				if (method.getParameterCount() != 0 && Event.class.isAssignableFrom(method.getParameterTypes()[0]))
				{
					method.setAccessible(true);
					log.debug("Got Event was: " + method.getParameters()[0].getType());
					try
					{
						log.debug("got called");
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
