/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.config;

public class RoundConfig extends BaseConfig
{
	/*Dynamic Properties*/
	public String mapName = "[z;p10]Crossing Large (10p)";
	public int mapType = 0;
	public float income = 1.0f;
	public int credits = 4000;
	public boolean disableNuke = true;
	public boolean sharedControl = false;
	public int fogType = 2;
	public int startingUnits = 1;
	//public String configName = "round.yml";
	
	public RoundConfig() {
		this.configName = "round.yml";
	}

	/** Creates an independent room configuration from the loaded defaults. */
	public RoundConfig(RoundConfig source) {
		this();
		if (source == null) {
			return;
		}
		this.mapName = source.mapName;
		this.mapType = source.mapType;
		this.income = source.income;
		this.credits = source.credits;
		this.disableNuke = source.disableNuke;
		this.sharedControl = source.sharedControl;
		this.fogType = source.fogType;
		this.startingUnits = source.startingUnits;
	}
}
