package io.rukkit;
import java.util.*;
import java.io.*;

public class ServerProperties
{
	private static Properties per = new Properties();
	
	public static void storageProperties() throws IOException{
		per.put("server-user", serverUser);
		per.put("server-motd", serverMotd);
		per.put("min-start-player", ""+minStartPlayer);
		try{
			per.put("server-uuid",/* "bad8b8ab-335e-475f-9953-7ac311be7f33"*/ java.util.UUID.randomUUID().toString());
		}catch(Exception e){
			per.put("server-uuid", "00000000-0000-0000-0000-000000000000");
		}
		per.put("max-player", "" + maxPlayer);
		per.put("debug", "false");
		per.put("online-mode", "false");
		per.put("single-player-mode", "false");
		per.store(new FileWriter("server.properties"), "This is rukkit profiles.");
	}
	
	public static void readProperties() throws IOException{
		per.load(new FileReader("server.properties"));
		serverUser = per.getProperty("server-user", "RUKKIT");
		serverMotd = per.getProperty("server-motd", "My Rukkit Server");
		minStartPlayer = Integer.parseInt(per.getProperty("min-start-player", "4"));
		UUID = per.getProperty("server-uuid", "00000000-0000-0000-0000-000000000000");
		if(per.getProperty("debug", "false").equals("true")){
			isDebug = true;
		}else{
			isDebug = false;
		}
		if(per.getProperty("online-mode", "false").equals("true")){
			onlineMode = true;
		}else{
			onlineMode = false;
		}
		if(per.getProperty("single-player-mode", "false").equals("true")){
			singlePlayerMode = true;
		}else{
			singlePlayerMode = false;
		}
	}
	
	/*Dynamic Properties*/
	public static String mapName = "[z;p10]Crossing Large (10p)";
	public static int mapType = 0;
	public static float income = 1.0f;
	public static int credits = 4000;
	public static boolean disableNuke = true;
	public static boolean sharedControl = false;
	public static int fogType = 2;
	public static int startingUnits = 1;
	
	/*Static Properties*/
	public static String serverUser = "RUKKIT";
	public static String serverMotd = "My Rukkit server";
	public static int maxPlayer = 10;
	public static int gameVersion = 143;
	public static int maxWatcher = 5;
	public static int minStartPlayer = 4;
	public static boolean singlePlayerMode = false;
	public static boolean isDebug = true;
	public static boolean onlineMode = false;
	public static String unitPath = "unitmeta.conf";
	public static String logPath = "/sdcard/rukkit-error.log";
	public static int maxPacketFrame = 8192;
	public static String UUID = "00000000-0000-0000-0000-000000000000";
}
