/*
 *  All Rights Reserved.
 *  FileName: RukkitLauncher.java
 *  @author: wtbdev
 *  @date: 2022/1/30 下午4:37
 */

package cn.rukkit;
import java.io.*;
import org.slf4j.*;
import io.netty.util.internal.logging.*;

public class RukkitLauncher
{
	public static void main(String args[]){
		InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
		try {
			Rukkit.startServer();
		} catch (IOException e) {
			//e.printStackTrace();
		} catch (InterruptedException e) {
			//e.printStackTrace();
		}
	}
	
}
