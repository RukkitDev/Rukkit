package io.rukkit.plugin;
import io.rukkit.util.*;
import java.io.*;
import java.util.*;
import java.util.jar.*;

public class PluginLoader
{
	private static String PLUGIN_FOLDER= "plugins";
	private static Logger log = new Logger("PluginLoader");
	
	public void loadPlugin() throws IOException{
		File folder = new File(PLUGIN_FOLDER);
		folder.mkdir();
		if(!folder.isDirectory()){
			log.w("Not a dir.Change a another dir...");
			folder = new File(PLUGIN_FOLDER + "_rukkit");
			folder.mkdir();
			if(!folder.exists() || !folder.isDirectory()){
				log.e("Load failed.Stop working.");
				return;
			}
		}
		
		for(File f: folder.listFiles()){
			String name = f.getName();
			String[] names = name.split("\\.");
			//log.d(f.getName()+names.toString());
			if(names[names.length - 1].equals("jar")){
				JarFile jar = new JarFile(f.getPath());
				Enumeration<JarEntry> e = jar.entries();
				while(e.hasMoreElements()){
					JarEntry entry = e.nextElement();
					if(entry.getName().contains("rukkitmeta.properties")){
						//System.out.println("go");
						Properties per = new Properties();
						per.load(jar.getInputStream(entry));
						String pluginName = per.getProperty("plugin-name", "null");
						String pluginVersion = per.getProperty("plugin-version", "null");
						log.i("Loading plugin: " + pluginName + " v" + pluginVersion);
					}
				}
			}
		}
	}
	
}
