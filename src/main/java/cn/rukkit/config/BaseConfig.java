/*
 *  All Rights Reserved.
 *  FileName: BaseConfig.java
 *  @author: wtbdev
 *  @date: 2022/1/30 下午4:37
 */

package cn.rukkit.config;

import cn.rukkit.*;
import java.io.*;
import org.yaml.snakeyaml.*;
//import java.util.logging.*;
import org.slf4j.*;

public abstract class BaseConfig
{
	Logger log = LoggerFactory.getLogger(this.getClass());
	public String configName;
	//public abstract void ConfigFile();
	public BaseConfig loadConfig() throws IllegalAccessException, InstantiationException, IOException {
		this.getClass().newInstance();
		File confFile = new File(Rukkit.getEnvPath() + configName);
		if (confFile.exists() && confFile.isFile()) {
			log.debug("Found Config file.Reading...");
			return new Yaml().loadAs(new FileReader(confFile), this.getClass());
		} else {
			log.debug("Config file.not found.Creating...");
			confFile.delete();
			confFile.createNewFile();
			BaseConfig cfg = this.getClass().newInstance();
			FileWriter writer = new FileWriter(confFile);
			writer.write(new Yaml().dumpAs(cfg, null, DumperOptions.FlowStyle.BLOCK));
			writer.flush();
			writer.close();
			return cfg;
		}
	}
}
