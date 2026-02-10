/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit;

import ch.qos.logback.classic.layout.TTLLLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.Layout;
import cn.rukkit.command.ServerCommandCompleter;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

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
					log.error("An unexpected error occurred in main loop", e);
				}
			}
		} catch (IOException e) {
			log.error("IO Exception during startup", e);
		} catch (InterruptedException e) {
			log.error("Interrupted during startup", e);
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
