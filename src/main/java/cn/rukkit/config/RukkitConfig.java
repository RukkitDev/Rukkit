/*
 *  All Rights Reserved.
 *  FileName: RukkitConfig.java
 *  @author: wtbdev
 *  @date: 2022/1/30 下午4:37
 */

package cn.rukkit.config;

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
