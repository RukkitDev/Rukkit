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
	public String mapName = "[p8]Two Sides (8p)";
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
}
