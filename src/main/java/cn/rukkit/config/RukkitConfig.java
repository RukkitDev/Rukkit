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
	public String welcomeMsg = "Welcome to Rukkit server, {playerName}!";
	public String serverMotd = "My Rukkit server";
	public int serverPort = 5123;
	public int maxPlayer = 10;
	public int maxRoom = 5;
	public int minStartPlayer = 4;
    public boolean syncEnabled = true;
	public boolean singlePlayerMode = false;
	public boolean isDebug = true;
	public boolean onlineMode = false;
	public String logPath = "/sdcard/rukkit-error.log";
	public int maxPacketFrame = 8192;
	public String UUID = "00000000-0000-0000-0000-000000000000";
	public String lang = Locale.getDefault().toString();
	//max threads in manager.Default = 8;
	public int threadPoolCount = 8;
	// max unit in per player 单玩家最大单位
	public int maxUnitsPerPlayer = 250;
	// using question system to vote 投票系统使用提示框模式实现
	// public boolean usingPopupInVote = false;
	
	//Ping packet receive timeout.default = 8000 (ms)
	public int pingTimeout = 8000;
	
	//registerTimeout default = 5 (s)
	public int registerTimeout = 5;
	
	//Using commandQuere to manage game commands
	public boolean useCommandQuere = false;

	public boolean checksumSync = false;
	
	public RukkitConfig() {
		this.configName = "rukkit.yml";
	}
}
