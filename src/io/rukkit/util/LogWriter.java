package io.rukkit.util;

import io.rukkit.command.*;
import java.util.*;
import java.io.*;
import io.rukkit.*;

public class LogWriter
{
	public static volatile LinkedList<String> commandQuere = new LinkedList<String>();
	
	private static FileWriter writer;
	
	public static void addLog(String s) {
		File f = new File(System.getProperty("user.dir") + "/lastrun.log");
		try
		{
			if (f.length() > 1024*1024*2 /* 2M */) {
				Date d = new Date();
				new File(System.getProperty("user.dir") + "/rukkit_last.log").delete();
				f.renameTo(new File(System.getProperty("user.dir") + "/rukkit_last.log"));
				f = new File(System.getProperty("user.dir") + "/lastrun.log");
				f.createNewFile();
				writer = new FileWriter(f);
			}
			writer.append(s);
			writer.flush();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static void startLoggerService() {
		File f = new File(System.getProperty("user.dir") + "/lastrun.log");
		try
		{
			if (f.exists()) f.delete();
			f.createNewFile();
			writer = new FileWriter(f);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
