/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.config;

import java.util.Locale;

public class RukkitConfig extends BaseConfig
{
	public String serverUser = "RUKKIT";
	public String welcomeMsg = "Welcome to Rukkit server, %s!";
	public String serverMotd = "My Rukkit server";
	public int serverPort = 5123;
	public int maxPlayer = 10;
	public int gameVersion = 143;
	public int maxWatcher = 5;
	public int minStartPlayer = 4;
    public boolean nonStopMode = false;
    public boolean syncEnabled = true;
	public boolean singlePlayerMode = false;
	public boolean isDebug = true;
	public boolean onlineMode = false;
	public String unitPath = "unitmeta.conf";
	public String logPath = "/sdcard/rukkit-error.log";
	public int maxPacketFrame = 8192;
	public String UUID = "00000000-0000-0000-0000-000000000000";
	public String nonStopDefaultMap = "";
	public int nonStopDefaultMapType = 0;
	public String lang = Locale.getDefault().toString();
	//private String configName = "rukkit.yml";
	//max threads in manager.Default = 8;
	public int threadPoolCount = 8;
	
	//Ping packet receive timeout.default = 8000 (ms)
	public int pingTimeout = 8000;
	
	//registerTimeout default = 5 (s)
	public int registerTimeout = 5;
	
	//Using commandQuere to manage game commands
	public boolean useCommandQuere = false;
	
	public RukkitConfig() {
		this.configName = "rukkit.yml";
	}
}
