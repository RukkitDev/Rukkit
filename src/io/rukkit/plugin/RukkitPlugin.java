package io.rukkit.plugin;
import io.rukkit.*;
import io.rukkit.util.*;
import java.io.*;
import java.util.*;
import org.yaml.snakeyaml.*;

public abstract class RukkitPlugin implements Plugin
{
	String pluginName;
	String pluginVersion;
	String mainClass;
	boolean isEnabled;
	private Logger log;
	
	public RukkitPlugin(){}
	public PluginManager getPluginManager(){
		return Rukkit.getCurrentPluginManager();
		//return Rukkit.getCurrentPluginHandler();
	}
	
	public final void initPlugin(PluginLoader loader) {
		getPluginManager().loadPlugin(this);
		log = new Logger(this.getClass().toString());
	}
	
	public final void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
		if (isEnabled) {
			onEnable();
		} else {
			onDisable();
		}
	}
	
	public final boolean getEnabled() {
		return isEnabled;
	}
	
	public final String getPluginName() {
		return pluginName;
	}
	
	public final String getPluginVersion() {
		return pluginVersion;
	}
	
	public final String getMainClass() {
		return mainClass;
	}
	
	public final Logger getLogger() {
		return log;
	}
	
	public final File getConfigFile(String config)
	{
		File configDir = new File(Rukkit.getEnvPath() + "/plugins/"+getPluginName());
		
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
	
	public final void setConfig(String file, String key, Object value) throws FileNotFoundException, IOException {
		setConfig(new File(Rukkit.getEnvPath() + "/plugins/"+getPluginName() + "/" + file + ".yml"), key, null);
	}
	
	public final void getConfig(String file, String key) throws FileNotFoundException {
		getConfig(new File(Rukkit.getEnvPath() + "/plugins/"+getPluginName() + "/" + file + ".yml"), key, null);
	}
	
	public final void getConfig(String file, String key, Object defaultValue) throws FileNotFoundException {
		getConfig(new File(Rukkit.getEnvPath() + "/plugins/"+getPluginName() + "/" + file + ".yml"), key, defaultValue);
	}
	
	public final void setConfig(File file, String key, Object value) throws FileNotFoundException, IOException {
		Yaml yaml = new Yaml();
		LinkedHashMap<String, Object> li = new LinkedHashMap<String, Object>();
		if(yaml.load(new FileInputStream(file)) != null) {
			li = yaml.load(new FileInputStream(file));
		}
		/*for (Map.Entry<String, Object> entry : li.entrySet()) {
			log.d("KEY: " + entry.getKey() + " VALUE: " + entry.getValue());
		}*/
		li.put(key, value);
		yaml.dump(li, new FileWriter(file));
	}

	public final Object getConfig(File file, String key, Object defaultValue) throws FileNotFoundException {
		Yaml yaml = new Yaml();
		LinkedHashMap<String, Object> li = new LinkedHashMap<String, Object>();
		li = yaml.load(new FileInputStream(file));
		/*for (Map.Entry<String, Object> entry : li.entrySet()) {
			log.d("KEY: " + entry.getKey() + " VALUE: " + entry.getValue());
		}*/
		try {
			return li.getOrDefault(key, defaultValue);
		} catch (NullPointerException e) {
			return null;
		}
	}
	
	public abstract void onLoad();
	public abstract void onEnable();
	public abstract void onDisable();
	public abstract void onServerDone();
	public abstract void onStop();
}
