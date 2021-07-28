package cn.rukkit;
import cn.rukkit.command.*;
import cn.rukkit.config.*;
import cn.rukkit.network.*;
import java.io.*;
import org.slf4j.*;
import org.yaml.snakeyaml.*;
import cn.rukkit.plugin.*;
import cn.rukkit.game.mod.*;
import cn.rukkit.plugin.internal.*;

public class Rukkit {
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

	public static void shutdown(String message) {
		// TODO: Implement this method
	}

	/**
	 *  load Plugin.
	 */
	public static final void loadPlugin() {

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

	public static final void loadRukkitConfig() throws IOException {
		if (config != null) return;
		config = getConfig("rukkit.yml", RukkitConfig.class);
	}

	public static final void loadRoundConfig() throws IOException {
		if (round != null) return;
		round = getConfig("round.yml", RoundConfig.class);
	}
	
	public static final <T> T getConfig(String path, Class<T> cls) throws FileNotFoundException, IOException {
		Yaml yaml = new Yaml();
		File confFile = new File(getEnvPath() + "/" + path);
		if (confFile.exists() && confFile.isFile()) {
			log.debug("Found Config file.Reading...");
		} else {
			log.debug("Config file.not found.Creating...");
			confFile.delete();
			confFile.createNewFile();
			FileWriter writer = new FileWriter(confFile);
			writer.write(yaml.dumpAs(cls, null, DumperOptions.FlowStyle.BLOCK));
			writer.flush();
			writer.close();
		}
		return yaml.loadAs((new FileInputStream(path)), cls);
	}
	
	public static final <T> T getConfig(File confFile, Class<T> cls) throws FileNotFoundException, IOException {
		return getConfig(confFile.getPath(), cls);
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
	
	public static final ModManager getModManager() {
		return modManager;
	}

	/**
	 * Start a Rukkit server.
	 */
	public static final void startServer() throws IOException {
		long time = System.currentTimeMillis();
		log.info("Loading server config...");
		loadRukkitConfig();
		log.info("Loading default round config...");
		loadRoundConfig();
		log.info("init::ModManager");
		modManager = new ModManager();
		modManager.loadInternalMod();
		modManager.loadAllModsInDir();
		log.info("init::CommandManager");
		commandManager = new CommandManager();
		log.info("init::GameServer");
		server = new GameServer(config.serverPort);
		log.info("init::ConnectionManager");
		connectionManager = new ConnectionManager(server);
		log.info("init::PluginManager");
		pluginManager = new PluginManager();
		pluginManager.loadPlugin(new CommandPlugin());
		pluginManager.loadPluginInDir();
		
		//log.info("init::
		//server.action(time);
	}
}
