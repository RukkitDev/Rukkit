package io.rukkit.command;
import io.rukkit.*;
import io.rukkit.util.*;
import io.rukkit.entity.*;

public class ConsoleCommand
{
	private static Logger log = new Logger("ConsoleCommand");
	public static void executeCommand(String command){
		log.d("Executed:" + command);
		String cmd[] = command.split(" ");
		switch (cmd[0]) {
			case "version": 
				log.c("You are running Rukkit v" + Rukkit.RUKKIT_VERSION + ".");
				log.c("Rukkit plugin API v" + Rukkit.RUKKIT_API_VERSION + ".");
				log.c("Server support RustedWarfare v" + Rukkit.GAME_SUPPORT_VERSION + "(" + Rukkit.GAME_SUPPORT_VERSION_CODE + ").");
				break;
			case "list":
				log.c(String.format("There are %d/%d players online.", PlayerGroup.size(), ServerProperties.maxPlayer));
				for (Player p: PlayerGroup.getPlayers()) {
					try {
					log.c(String.format("[Slot %d] %s (Team: %d) uuid: %s", p.playerIndex, p.playerName, p.playerTeam, p.UUID));
					} catch (NullPointerException e) {
						continue;
					}
				}
				break;
			case "stop":
				Rukkit.shutdown("Console executed command.");
				break;
			case "help":
				log.c("--Rukkit console help-- \n" +
				"list: playerList." +
				"stop: Stop server." +
				"version: show version info." +
				"mods: list mods." 
					);
				break;
			default:
				log.c("Unknown command.");
		}
	}
}
