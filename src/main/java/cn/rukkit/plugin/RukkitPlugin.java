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
import java.io.*;
import java.util.ArrayList;

import cn.rukkit.event.EventListener;
import org.slf4j.*;
import org.yaml.snakeyaml.*;
import org.yaml.snakeyaml.nodes.Tag;

public abstract class RukkitPlugin implements Plugin
{
	public PluginConfig config;
	protected ArrayList<EventListener> listeners = new ArrayList<EventListener>();
	
	private boolean isEnabled;
	
	public RukkitPlugin() {}
	public final void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
		if (isEnabled) {
			onEnable();
		} else {
			onDisable();
		}
	}
	
	public final boolean isEnabled() {
		return isEnabled;
	}
	
	public final Logger getLogger() {
		return LoggerFactory.getLogger(this.getPluginId() + "-" + this.getClass());
	}
	
	public final File getConfigFile(String config)
	{
		File configDir = new File(Rukkit.getEnvPath() + "/plugins/"+ this.config.name);

		if (configDir.isFile()) {
			configDir.delete();
		}

		if (!configDir.exists()) {
			configDir.mkdir();
		}

		File configFile = new File(configDir + "/" + config + ".yml");
		if (configFile.isDirectory()) {
			configFile.delete();
		}

		if (!configFile.exists()) {
			try
			{
				configFile.createNewFile();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}

		return configFile;
	}
	
	public final <T> T getConfig(File file, Class<T> cls) throws FileNotFoundException {
		Yaml yaml = new Yaml();
		return yaml.loadAs((new FileInputStream(file)), cls);
	}

	public final void saveConfig(File file, Object cls) throws IOException {
		Yaml yaml = new Yaml();
		FileWriter writer = new FileWriter(file);
		writer.write(yaml.dumpAs(cls, Tag.MAP, DumperOptions.FlowStyle.BLOCK));
		writer.flush();
		writer.close();
	}

	public String getPluginId() {
		return config.id;
	}
	
	public PluginManager getPluginManager() {
		return Rukkit.getPluginManager();
	}
}
