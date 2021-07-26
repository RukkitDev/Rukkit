package cn.rukkit;
import cn.rukkit.command.*;
import cn.rukkit.config.*;
import cn.rukkit.network.*;
import java.io.*;
import org.slf4j.*;
import org.yaml.snakeyaml.*;
import cn.rukkit.plugin.*;
import cn.rukkit.game.mod.*;

public class Rukkit
{
	public static final String RUKKIT_VERSION = "0.7.0";
	public static final int SUPPORT_GAME_VERSION = 151;
	private static Logger log = LoggerFactory.getLogger(Rukkit.class);
	private static RoundConfig round;
	private static RukkitConfig config;
	
	private static CommandManager commandManager;

	private static ConnectionManager connectionManager;
	
	private static PluginManager pluginManager;
	
	private static ModManager modManager;
	
	private static GameServer server;

	public static void shutdown(String message)
	{
		// TODO: Implement this method
	}
	
	/**
    *  load Plugin.
    */
	public static final void loadPlugin(){
		
	}
	
	/**
	* Returns current gameInstance.
	*/
	public static final void getGameInstance() {
		
	}
	
	/**
	* Get a rukkit config.
	* {@link RukkitConfig}
	*/
	public static final RukkitConfig getConfig() {
		return config;
	}
	
	/**
	* Get a ingame config.
	* Including map, credits, etc. {@link RoundConfig}
	*/
	public static final RoundConfig getRoundConfig() {
		return round;
	}
	
	public static final CommandManager getCommandManager() {
		return commandManager;
	}
	
	public static final ConnectionManager getConnectionManager() {
		return connectionManager;
	}
	
	public static final GameServer getGameServer() {
		return server;
	}
	
	public static final void loadRukkitConfig() throws IOException, IllegalAccessException, InstantiationException {
		if (config != null) return;
		config = (RukkitConfig) new RukkitConfig().loadConfig();
		/*File confFile = new File(getEnvPath() + "/rukkit.yml");
		if (confFile.exists() && confFile.isFile()) {
			log.debug("Found Config file.Reading...");
			config = new Yaml().loadAs(new FileReader(confFile), RukkitConfig.class);
		} else {
			log.debug("Config file.not found.Creating...");
			confFile.delete();
			confFile.createNewFile();
			RukkitConfig cfg = new RukkitConfig();
			FileWriter writer = new FileWriter(confFile);
			writer.write(new Yaml().dumpAs(cfg, null, DumperOptions.FlowStyle.BLOCK));
			writer.flush();
			writer.close();
			config = cfg;
		}*/
	}
	
	public static final void loadRoundConfig() throws IOException {
		if (round != null) return;
		File confFile = new File(getEnvPath() + "/server.yml");
		if (confFile.exists() && confFile.isFile()) {
			log.debug("Found Config file.Reading...");
			round = new Yaml().loadAs(new FileReader(confFile), RoundConfig.class);
		} else {
			log.debug("Config file.not found.Creating...");
			confFile.delete();
			confFile.createNewFile();
			RoundConfig cfg = new RoundConfig();
			Yaml yaml = new Yaml();
			FileWriter writer = new FileWriter(confFile);
			writer.write(yaml.dumpAs(cfg, null, DumperOptions.FlowStyle.BLOCK));
			writer.flush();
			writer.close();
			round = cfg;
		}
	}
	
	public static final String getEnvPath() {
		return System.getProperty("user.dir");
	}
	
	/**
	* Start Server ignore config port.
	* @param port server port.
	*/
	public static final void startServer(int port) {
		
	}
	
	/**
	* Start a Rukkit server.
	*/
	public static final void startServer() throws IOException, InterruptedException, IllegalAccessException, InstantiationException {
		long time = System.currentTimeMillis();
		log.info("Loading server config...");
		loadRukkitConfig();
		log.info("Loading default round config...");
		loadRoundConfig();
		server = new GameServer(config.serverPort);
		log.info("init::ConnectionManager");
		connectionManager = new ConnectionManager(server);
		log.info("init::GameServer");
		server.action(time);
	}
}
