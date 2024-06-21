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
import java.text.MessageFormat;
import java.util.*;
import java.util.jar.*;

import cn.rukkit.util.LangUtil;
import org.slf4j.*;
import cn.rukkit.network.*;
import org.yaml.snakeyaml.*;

public class PluginManager 
{
	private static String PLUGIN_FOLDER= Rukkit.getEnvPath() + "/plugins";
	private static Logger log = LoggerFactory.getLogger(PluginManager.class);
	private static boolean isServerDone = false;
	private static HashMap<String, RukkitPlugin> pluginMap = new LinkedHashMap<>();
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

						// 插件无效，未配置id
						if (pluginConf.id == null) {
							log.warn(MessageFormat.format(LangUtil.getString("rukkit.plugin.invalid"), f.getName()));
							break;
						}

						if (pluginConf.pluginClass == null)
						{
							log.error("Plugin " + pluginConf.name + " not have a Main class.Please check your properties file.");
							break;
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
						}
						catch (IllegalAccessException e1)
						{
							log.error("Plugin could not be loaded.",e1);
						}
						catch (InstantiationException e2)
						{
							log.error("Plugin could not be loaded.", e2);
						}
						catch (ClassNotFoundException e3)
						{
							log.error("Plugin could not be loaded.Class not found.", e3);
						}
					}
				}
			}
		}

		enableAllPlugins();
		log.info("Load complete!");
	}

	public void serverDone(RoomGameServer g) {
		if (isServerDone) {
			throw new RuntimeException("Server already started.");
		} else {
			isServerDone = true;
			for(RukkitPlugin p : pluginMap.values()) {
				p.onDone();
			}
		}
	}

	//加载单个插件类
	public void loadPlugin(RukkitPlugin plugin)
	{
		if (plugin.getPluginId() == null) {
			log.warn(MessageFormat.format(LangUtil.getString("rukkit.plugin.invalid"), "<Dynamic Plugin>"));
		}

		log.info("Loading plugin: " + plugin.config.id);
		PluginConfig pluginConf = plugin.config;

		// 插件配置文件检查
		if (pluginMap.getOrDefault(pluginConf.id, null) != null) {
			log.error("Plugin {} already loaded!", pluginConf.id);
			return;
		}

		if (pluginConf.name == null) {
			log.warn("Plugin {} not have a name!Check plugin.yml to configure it!", pluginConf.id);
			pluginConf.name = pluginConf.id;
		}

		if (pluginConf.version == null) {
			log.warn("Plugin {} not have a version!Check plugin.yml to configure it!", pluginConf.id);
			pluginConf.version = "null";
		}

		if (pluginConf.author == null) {
			log.warn("Plugin {} not have a author!Check plugin.yml to configure it!", pluginConf.id);
			pluginConf.author = "null";
		}

		if (pluginConf.apiVersion == null) {
			log.error("Plugin {} not have a apiVersion!This will cause incompatible problems!Plugin will not be loaded!", pluginConf.id);
			return;
		}

		if (!pluginConf.apiVersion.equals(Rukkit.PLUGIN_API_VERSION)) {
			log.warn("Plugin {} have a incompatible apiVersion:{}, This may cause incompatible problem in current API Version:{}!",
					plugin.getPluginId(), pluginConf.apiVersion, Rukkit.PLUGIN_API_VERSION);
		}

		if (pluginMap.getOrDefault(plugin.config.id, null) == null)
		{
			plugin.onLoad();
			pluginMap.put(plugin.config.id, plugin);
		}
		else
		{
			log.warn("Plugin " + plugin.config.name + " had already added.");
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
		RukkitPlugin pluginToEnable = pluginMap.getOrDefault(plugin.config.id, null);
		if (pluginToEnable == null) {
			log.warn("Cannot enable plugin {} because this plugin didn't load!", plugin.getPluginId());
		} else {
			pluginToEnable.setEnabled(true);
		}
	}

	//禁用已加载的插件
	public void disablePlugin(RukkitPlugin plugin)
	{
		RukkitPlugin pluginToDisable = pluginMap.getOrDefault(plugin.config.id, null);
		if (pluginToDisable == null) {
			log.warn("Cannot enable plugin {} because this plugin didn't load!", plugin.getPluginId());
		} else {
			pluginToDisable.setEnabled(false);
			for (EventListener listener: pluginToDisable.listeners) {
				unregisterEventListener(listener);
			}
		}
	}

	//禁用所有插件
	public void disableAllPlugins()
	{
		for (RukkitPlugin plugin: pluginMap.values())
		{
			disablePlugin(plugin);
		}
	}

	//启用所有插件
	void enableAllPlugins()
	{
		for (RukkitPlugin plugin: pluginMap.values())
		{
			enablePlugin(plugin);
		}
	}

	/**
	 * 注册一个EventListener内所有的事件监听
	 * @param listener 注册的EventListener
	 * @param plugin 注册插件
	 */
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
					log.trace("Got Event was: " + method.getParameters()[0].getType());
					try
					{
						log.trace("got called");
						ListenerList list = ((ListenerList) method.getParameters()[0].getType().getMethod("getListenerList").invoke(null));
						EventListenerContainer container = new EventListenerContainer(plugin, method, listener);
						plugin.listeners.add(listener);
						list.registerListener(container);
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

	/**
	 * 取消注册一个EventListener里面所有的事件监听。
	 * @param listener
	 */
	public void unregisterEventListener(EventListener listener)
	{
		Method methods[] = listener.getClass().getMethods();
		log.debug(" ");
		for (Method method: methods)
		{
			log.trace("get");
			//判断方法是否进行注解
			if (method.isAnnotationPresent(EventHandler.class) && !method.isSynthetic())
			{
				//log.d("isAnno");
				//判断方法第一个参数是否为Event
				if (method.getParameterCount() != 0 && Event.class.isAssignableFrom(method.getParameterTypes()[0]))
				{
					method.setAccessible(true);
					log.trace("Got Event was: " + method.getParameters()[0].getType());
					try
					{
						log.trace("got called");
						ListenerList list = ((ListenerList) method.getParameters()[0].getType().getMethod("getListenerList").invoke(null));
						list.removePluginListener(listener);
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
