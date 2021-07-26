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
}
