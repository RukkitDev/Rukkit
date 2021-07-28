package cn.rukkit;
import java.io.*;
import org.slf4j.*;
import io.netty.util.internal.logging.*;

public class RukkitLauncher
{
	public static void main(String args[]) throws IOException{
		InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
		Rukkit.startServer();
	}
	
}
