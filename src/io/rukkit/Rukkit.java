package io.rukkit;
import io.rukkit.map.*;
import io.rukkit.net.*;
import io.rukkit.plugin.*;
import io.rukkit.util.*;
import java.io.*;
import java.util.*;
import io.rukkit.entity.*;
import io.rukkit.mod.*;

public class Rukkit
{
	private static Game game;
	public static RukkitProperties config;
	public static final String GAME_SUPPORT_VERSION = "1.14";
	public static final int GAME_SUPPORT_VERSION_CODE = 151;
	public static final String RUKKIT_VERSION = "0.5.52-dev";
	public static final String RUKKIT_API_VERSION = "0.2.51-dev";
	private static RukkitConsoleHandler console = null;
	private static ArrayList<ModUnit> internalModUnits = new ArrayList<ModUnit>();
	private static PluginManager PluginManager = new PluginManager();
	private static ModManager modManager = new ModManager();
	private static String dirPath = System.getProperty("user.dir");
	private static Logger log = new Logger("RukkitLauncher");
	
	public static ModManager getModManager() {
		return modManager;
	}
	
	public static String getEnvPath() {
		return System.getProperty("user.dir");
	}
	
	public static ArrayList<ModUnit> getInternalModUnitsList() {
		return internalModUnits;
	}
	
	public static void setConsole(RukkitConsoleHandler mConsole) {
		if(console == null) console = mConsole;
	}
	
	public static PluginManager getCurrentPluginManager()
	{
		return PluginManager;
	}
	
	public static Game getGame() {
		return game;
	}

	public static void main(String args[]) throws InterruptedException, Exception
	{
		//dirPath = System.getProperty("user.dir");
		LogWriter.startLoggerService();
		log.d(dirPath);
		log.i("Rukkit server is preparing...");
		log.i("You are running Rukkit v" + RUKKIT_VERSION + ".");
		log.i("Rukkit plugin API v" + RUKKIT_API_VERSION + ".");
		log.i("Server support RustedWarfare v" + GAME_SUPPORT_VERSION + "(" + GAME_SUPPORT_VERSION_CODE + ").");
		Long time = System.currentTimeMillis();
		if (new File(dirPath + "/server.properties").isFile() && new File(dirPath + "/server.properties").exists())
		{
			log.i("Loading server.properties...");
			ServerProperties.readProperties();
			config = RukkitProperties.readProfile();
		}
		else
		{
			log.i("Properties not found.Creating server.properties...");
			ServerProperties.storageProperties();
		}
		if (!CustomMapLoader.createDir())
		{
			log.e("Map folder create failed.Server Stopped!");
		}
		if (ServerProperties.onlineMode)
		{
			log.w("You are running on online mode.Some network plugins will start at loading.");
		}
		log.i("Loading unit meta data...");
		try {
			readDefaultModUnits();
		} catch (IOException e) {
			log.e(e);
			shutdown("Unit data load failed.");
		}
		log.i("Loading mods..");
		modManager.loadAllModsInDir();
		modManager.enableAllMods();
		log.i("Game server ID is : " + ServerProperties.UUID);
		log.i("Starting server on 0.0.0.0:" + ServerProperties.serverPort);
		PluginManager.loadPluginInDir();
		(game = new Game(ServerProperties.serverPort)).action(time);

	}
	
	private static void readDefaultModUnits() throws IOException {
		internalModUnits = modManager.loadInternalMod();
//		BufferedReader reader = new BufferedReader(new InputStreamReader(Rukkit.class.getClassLoader().getResourceAsStream("unitmeta.conf")));
//		LinkedList<String> li = new LinkedList<String>();
//		String b = null;
//		while ((b = reader.readLine()) != null)
//		{
//			li.addLast(b);
//		}
//		//li.size();
//		for(String c: li){
//			String unitdata[] = c.split("%#%");
//			ModUnit unit = new ModUnit(unitdata[0], Integer.parseInt(unitdata[1]), unitdata[2]);
//			if (unit.getModName().equals("default")) {
//				internalModUnits.add(unit);
//			}
//		}
	}
	
	public static void shutdown(String reason) {
		log.w("Server stopped.Reason: " + reason);
		shutdown();
	}
	
	public static void shutdown() {
		log.w("Server stopping..");
		console.shutdown();
		System.exit(0);
	}
}
