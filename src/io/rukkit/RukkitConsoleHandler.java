package io.rukkit;
import java.util.*;
import io.rukkit.command.*;
import io.rukkit.util.*;

public class RukkitConsoleHandler extends Thread
{
	public void shutdown() {
		interrupt();
	}
	
	@Override
	public void run()
	{
		new Logger("Console").w("Console have OOM problems.Now disabled.");
		/*Scanner scanner = new Scanner(System.in);
		while (!interrupted()) {
			//System.out.printf("\033[1mConsole >\033[0m");
			String s = scanner.next();
			ConsoleCommand.executeCommand(s);
		}*/
	}
}
