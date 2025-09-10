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
import java.awt.*;
import javax.swing.*;

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
				}catch (UserInterruptException e) {
					log.info("Stopping server...");
					Rukkit.shutdown("Server stopped by console");
				}catch (EndOfFileException e) {
					log.info("Stopping server...");
					Rukkit.shutdown("Server stopped by console");
					break;
				}catch (Exception e) {
					System.out.println("Oops.A exception occurred.");
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			//e.printStackTrace();
		} catch (InterruptedException e) {
			//e.printStackTrace();
		} catch (NoClassDefFoundError e){
			//终端有时会有这个bug 导致无法退出进程
			//e.printStackTrace();
		}
	}

	public static void mainx(String args[]){
		JFrame a =new JFrame("hello");
		a.setBounds(600, 500, 700, 500);
		java.awt.Button b =new Button("ffff");
		b.setBackground(Color.BLACK);
		//a.add(b);
		a.add(b);
		a.setVisible(true);
	}
	Layout<ILoggingEvent> layout = new TTLLLayout();

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

/**
 *                             _ooOoo_
 *                            o8888888o
 *                            88" . "88
 *                            (| -_- |)
 *                            O\  =  /O
 *                         ____/`---'\____
 *                       .'  \\|     |//  `.
 *                      /  \\|||  :  |||//  \
 *                     /  _||||| -:- |||||-  \
 *                     |   | \\\  -  /// |   |
 *                     | \_|  ''\---/''  |   |
 *                     \  .-\__  `-`  ___/-. /
 *                   ___`. .'  /--.--\  `. . __
 *                ."" '<  `.___\_<|>_/___.'  >'"".
 *               | | :  `- \`.;`\ _ /`;.`/ - ` : | |
 *               \  \ `-.   \_ __\ /__ _/   .-` /  /
 *          ======`-.____`-.___\_____/___.-`____.-'======
 *                             `=---='
 *          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
 *                     佛祖保佑        永无BUG
 *            佛曰:
 *                   写字楼里写字间，写字间里程序员；
 *                   程序人员写程序，又拿程序换酒钱。
 *                   酒醒只在网上坐，酒醉还来网下眠；
 *                   酒醉酒醒日复日，网上网下年复年。
 *                   但愿老死电脑间，不愿鞠躬老板前；
 *                   奔驰宝马贵者趣，公交自行程序员。
 *                   别人笑我忒疯癫，我笑自己命太贱；
 *                   不见满街漂亮妹，哪个归得程序员？
*/