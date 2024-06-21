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
import java.util.Arrays;
import java.util.Scanner;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.layout.TTLLLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.encoder.Encoder;
import cn.rukkit.command.CommandManager;
import cn.rukkit.command.ServerCommandCompleter;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.slf4j.*;
import io.netty.util.internal.logging.*;
import org.jline.terminal.*;
import org.jline.reader.*;

public class RukkitLauncher extends ConsoleAppender<ILoggingEvent>
{
	static Terminal terminal;
	static LineReader lineReader;
	public static final String PATTERN = ">";
	static Thread terminalThread;
	static boolean isTerminalRunning = true;

	public static ServerCommandCompleter serverCommandCompleter = new ServerCommandCompleter();

	private static Logger log = LoggerFactory.getLogger("Launcher");
	public static void main(String args[]){
		InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
		try {
			log.info("init::JLine Terminal...");
			terminal = TerminalBuilder.builder().system(true).jna(true).build();
			lineReader = LineReaderBuilder.builder().terminal(terminal).completer(new ArgumentCompleter(
					serverCommandCompleter,
					NullCompleter.INSTANCE
			)).build();
			Rukkit.startServer();
			while (isTerminalRunning) {
				try {
					Thread.sleep(1); // 不知道为什么反正要这个东西
					if (Rukkit.getCommandManager() == null) continue;
					if (Rukkit.isStarted()) {
						serverCommandCompleter.setCommandCompleteVars(Rukkit.getCommandManager().getLoadedServerCommandStringList());
					} else {
						continue;
					}
					String str = lineReader.readLine(PATTERN);
					Rukkit.getCommandManager().executeServerCommand(str);
				}
				catch (UserInterruptException e) {
					log.info("Stopping server...");
					Rukkit.shutdown("Server stopped by console");
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
		} catch (IOException e) {
			//e.printStackTrace();
		} catch (InterruptedException e) {
			//e.printStackTrace();
		}
	}

	Layout<ILoggingEvent> layout = new TTLLLayout();

	private static void nop() {}


	@Override
	public void start() {
		super.start();
		layout.start();
	}

	@Override
	public void stop() {
		layout.stop();
		super.stop();
	}

	@Override
	protected void subAppend(ILoggingEvent event) {
		lineReader.printAbove(new String(encoder.encode(event)));
	}

}
