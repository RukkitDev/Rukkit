package cn.rukkit.plugin;
import cn.rukkit.*;
import java.io.*;
import org.slf4j.*;
import org.yaml.snakeyaml.*;
import cn.rukkit.config.*;

public abstract class RukkitPlugin implements Plugin
{
	public PluginConfig config;
	
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
		return LoggerFactory.getLogger(this.getClass());
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
}
