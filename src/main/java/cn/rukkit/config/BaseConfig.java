/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.config;

import cn.rukkit.*;
import java.io.*;
import org.yaml.snakeyaml.*;
//import java.util.logging.*;
import org.slf4j.*;
import org.yaml.snakeyaml.nodes.Tag;

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
			writer.write(new Yaml().dumpAs(cfg, Tag.MAP, DumperOptions.FlowStyle.BLOCK));
			writer.flush();
			writer.close();
			return cfg;
		}
	}
}
