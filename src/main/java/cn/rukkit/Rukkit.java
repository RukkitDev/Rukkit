/*
 *  All Rights Reserved.
 *  FileName: Rukkit.java
 *  @author: wtbdev
 *  @date: 2022/1/30 下午4:37
 */

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
import cn.rukkit.service.*;
import cn.rukkit.game.SaveManager;
import java.util.UUID;

public class Rukkit {
	public static final String RUKKIT_VERSION = "0.7.2";
	public static final int SUPPORT_GAME_VERSION = 151;
	private static Logger log = LoggerFactory.getLogger(Rukkit.class);
	private static RoundConfig round;
	private static RukkitConfig config;

	private static CommandManager commandManager;

	private static ConnectionManager connectionManager;

	private static PluginManager pluginManager;

	private static ModManager modManager;

	private static GameServer server;
	
	private static ThreadManager threadManager;
    
    private static SaveManager saveManager;

	public final static String PLUGIN_API_VERSION = "0.6.0";

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

	public static final ThreadManager getThreadManager() {
		return threadManager;
	}
	
	public static final CommandManager getCommandManager() {
		return commandManager;
	}

	public static final ConnectionManager getConnectionManager() {
		return connectionManager;
	}
    
    public static final SaveManager getSaveManager() {
        return saveManager;
    }

	public static final GameServer getGameServer() {
		return server;
	}
	
	public static final PluginManager getPluginManager() {
		return pluginManager;
	}

	public static final void loadRukkitConfig() throws IOException {
		if (config != null) return;
		Yaml yaml = new Yaml();
		File confFile = new File(getEnvPath() + "/rukkit.yml");
		if (confFile.exists() && confFile.isFile()) {
			log.debug("Found Config file.Reading...");
		} else {
			log.debug("Config file.not found.Creating...");
			confFile.delete();
			confFile.createNewFile();
			FileWriter writer = new FileWriter(confFile);
            RukkitConfig conf = new RukkitConfig();
            conf.UUID = UUID.randomUUID().toString();
			writer.write(yaml.dumpAs(conf, null, DumperOptions.FlowStyle.BLOCK));
			writer.flush();
			writer.close();
		}
		config = yaml.loadAs((new FileInputStream(confFile)), RukkitConfig.class);
	}

	public static final void loadRoundConfig() throws IOException {
		if (round != null) return;
		Yaml yaml = new Yaml();
		File confFile = new File(getEnvPath() + "/round.yml");
		if (confFile.exists() && confFile.isFile()) {
			log.debug("Found Config file.Reading...");
		} else {
			log.debug("Config file.not found.Creating...");
			confFile.delete();
			confFile.createNewFile();
			FileWriter writer = new FileWriter(confFile);
			writer.write(yaml.dumpAs(new RoundConfig(), null, DumperOptions.FlowStyle.BLOCK));
			writer.flush();
			writer.close();
		}
		round = yaml.loadAs((new FileInputStream(confFile)), RoundConfig.class);
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
	public static final void startServer() throws IOException, InterruptedException {
		long time = System.currentTimeMillis();
		log.info("Loading server config...");
		loadRukkitConfig();
		log.info("Loading default round config...");
		loadRoundConfig();
		log.info("init::ThreadManager");
		threadManager = new ThreadManager(config.threadPoolCount);
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
		if (config.nonStopMode) {
			pluginManager.loadPlugin(new NoStopCommandPlugin());
		} else {
			pluginManager.loadPlugin(new CommandPlugin());
		}
		pluginManager.loadPlugin(new TestPlugin());
		pluginManager.loadPluginInDir();
        //init SaveManager.
        saveManager = new SaveManager();
		
		log.info("start::game server on port:" + config.serverPort);
		server.action(time);
	}
}
