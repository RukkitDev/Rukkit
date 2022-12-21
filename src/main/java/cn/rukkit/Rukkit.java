/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit;
import cn.rukkit.command.*;
import cn.rukkit.config.*;
import cn.rukkit.network.*;
import java.io.*;

import cn.rukkit.network.packet.Packet;
import cn.rukkit.util.LangUtil;
import org.slf4j.*;
import org.yaml.snakeyaml.*;
import cn.rukkit.plugin.*;
import cn.rukkit.game.mod.*;
import cn.rukkit.plugin.internal.*;
import cn.rukkit.service.*;
import cn.rukkit.game.SaveManager;

import java.util.Locale;
import java.util.UUID;

public class Rukkit {
	private static boolean isStarted = false;
	public static final String RUKKIT_VERSION = "0.8.2-dev";
	public static final int SUPPORT_GAME_VERSION = 151;
	private static final Logger log = LoggerFactory.getLogger(Rukkit.class);
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
		log.info("Server will shutdown...");
		log.info("Disconnect current players...");
		if (getGameServer().isGaming()) {
			getConnectionManager().broadcastServerMessage("Server closed!");
			getConnectionManager().clearAllSaveData();
			getConnectionManager().disconnect();
		} else {
			try {
				getConnectionManager().broadcast(Packet.kick("Server closed."));
				getConnectionManager().disconnect();
			} catch (IOException e) {}
		}
		log.info("Stop ThreadManager...");
		getThreadManager().shutdown();
		log.info("Shutdown server...");
		getGameServer().stopServer();
		log.info("Stop terminal...");
		RukkitLauncher.isTerminalRunning = false;
		RukkitLauncher.terminalThread.interrupt();
	}

	/**
	 *  load Plugin.
	 */
	public static void loadPlugin() {
		
	}

	/**
	 * Returns current gameInstance.
	 */
	public static void getGameInstance() {

	}

	/**
	 * Set game server start state.
	 * Internal use, DO NOT Change it.
	 */
	public static void setStarted(Boolean isStart) {
		isStarted = isStart;
	}

	/**
	 * Get game server start state.
	 * Useful for plugin preparations.
	 */
	public static boolean isStarted() {
		return isStarted;
	}

	/**
	 * Get a rukkit config.
	 * {@link RukkitConfig}
	 */
	public static RukkitConfig getConfig() {
		return config;
	}

	/**
	 * Get a ingame config.
	 * Including map, credits, etc. {@link RoundConfig}
	 */
	public static RoundConfig getRoundConfig() {
		return round;
	}

	public static ThreadManager getThreadManager() {
		return threadManager;
	}

	public static CommandManager getCommandManager() {
		return commandManager;
	}

	public static ConnectionManager getConnectionManager() {
		return connectionManager;
	}

    public static SaveManager getSaveManager() {
        return saveManager;
    }

	public static GameServer getGameServer() {
		return server;
	}

	public static PluginManager getPluginManager() {
		return pluginManager;
	}

	public static void loadRukkitConfig() throws IOException {
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
		log.info("setting up language...");
		LangUtil.lc = new Locale(getConfig().lang.split("_")[0], getConfig().lang.split("_")[1]);
		log.info("Current Language: {}", LangUtil.lc);
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
		pluginManager.loadPlugin(new BasePlugin());
		if (config.nonStopMode) {
			pluginManager.loadPlugin(new NoStopCommandPlugin());
		} else {
			pluginManager.loadPlugin(new CommandPlugin());
		}
		pluginManager.loadPlugin(new TestPlugin());
		pluginManager.loadPlugin(new ServerCommandPlugin());
		pluginManager.loadPluginInDir();
        //init SaveManager.
        saveManager = new SaveManager();
		
		log.info("start::game server on port:" + config.serverPort);
		server.action(time);
	}
}
