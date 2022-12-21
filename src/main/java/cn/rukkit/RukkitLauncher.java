/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit;
import java.io.*;
import java.util.Scanner;

import org.slf4j.*;
import io.netty.util.internal.logging.*;
import org.jline.terminal.*;
import org.jline.reader.*;

public class RukkitLauncher
{
	static Terminal terminal;
	static LineReader lineReader;
	public static final String PATTERN = "console >";
	static Thread terminalThread;
	static boolean isTerminalRunning = true;

	private static Logger log = LoggerFactory.getLogger("Launcher");
	public static void main(String args[]){
		InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
		try {
			log.info("init::JLine Terminal...");
			terminal = TerminalBuilder.builder().system(true).jna(true).build();
			lineReader = LineReaderBuilder.builder().terminal(terminal).build();
			terminalThread = new Thread(new Runnable() {
				@Override
				public void run() {
					while (isTerminalRunning) {
						try {
							String str = lineReader.readLine(PATTERN);
							if (Rukkit.getCommandManager() == null) continue;
							Rukkit.getCommandManager().executeServerCommand(str);
						}
						catch (EndOfFileException e) {
							log.info("Stopping server...");
							Rukkit.shutdown("Server stopped by console");
							break;
						}
						catch (Exception e) {
							System.out.println("Oops.A exception occurred.");
							e.printStackTrace();
						}
					}
//					System.out.println("<Terminal stopped.>");
				}
			});
			terminalThread.start();
			Rukkit.startServer();
		} catch (IOException e) {
			//e.printStackTrace();
		} catch (InterruptedException e) {
			//e.printStackTrace();
		}
	}
	
}
