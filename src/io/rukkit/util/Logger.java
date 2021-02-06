package io.rukkit.util;

import java.util.*;
import io.rukkit.*;

public class Logger
{
	private String tag;
	
	public Logger(String tag){
		this.tag = tag;
	}
	
	public void i(Object obj){
		Date d = new Date();
		System.out.printf("\033[36m%tF %tT [INFO][%s] %s\033[0m\n", d, d, tag, obj.toString());
		LogWriter.addLog(String.format("%tF %tT [INFO][%s] %s\n", d, d, tag, obj.toString()));
	}
	
	public void w(Object obj){
		Date d = new Date();
		System.out.printf("\033[33m%tF %tT [WARN][%s] %s\033[0m\n", d, d, tag, obj.toString());
		LogWriter.addLog(String.format("%tF %tT [WARN][%s] %s\n", d, d, tag, obj.toString()));
	}
	
	public void e(Object obj){
		Date d = new Date();
		System.out.printf("\033[31m%tF %tT [ERROR][%s] %s\033[0m\n", d, d, tag, obj.toString());
		LogWriter.addLog(String.format("%tF %tT [ERROR][%s] %s\n", d, d, tag, obj.toString()));
	}
	
	public void w(Exception e) {
		Date d = new Date();
		StringBuffer buf = new StringBuffer();
		buf.append(String.format("%tF %tT [WARN][%s] %s\n", d, d, tag, "Expection caught: " + e));
		buf.append(String.format("%tF %tT [WARN][%s] %s\n", d, d, tag, "Message: " + e.getMessage()));
		//buf.append(String.format("%tF %tT [ERROR][%s] %s\n", d, d, tag, "Cause: " + e.getCause()));
		buf.append(String.format("%tF %tT [WARN][%s] %s\n", d, d, tag, "StackTrace: "));
		for (StackTraceElement element:  e.getStackTrace()) {
			buf.append(String.format("%tF %tT [WARN][%s] %s\n", d, d, tag, element.toString()));
		}
		LogWriter.addLog(buf.toString());
		System.out.printf("\033[33m" + buf.toString() + "\033[0m\n");
	}
	
	public void e(Exception e) {
		Date d = new Date();
		StringBuffer buf = new StringBuffer();
		buf.append(String.format("%tF %tT [ERROR][%s] %s\n", d, d, tag, "Expection caught: " + e));
		buf.append(String.format("%tF %tT [ERROR][%s] %s\n", d, d, tag, "Message: " + e.getMessage()));
		//buf.append(String.format("%tF %tT [ERROR][%s] %s\n", d, d, tag, "Cause: " + e.getCause()));
		buf.append(String.format("%tF %tT [ERROR][%s] %s\n", d, d, tag, "StackTrace: "));
		for (StackTraceElement element:  e.getStackTrace()) {
			buf.append(String.format("%tF %tT [ERROR][%s] %s\n", d, d, tag, element.toString()));
		}
		LogWriter.addLog(buf.toString());
		System.out.printf("\033[31m" + buf.toString() + "\033[0m\n");
	}
	
	public void d(Object obj){
		if(ServerProperties.isDebug){
			Date d = new Date();
			System.out.printf("\033[32m%tF %tT [DEBUG][%s] %s\033[0m\n", d, d, tag, obj.toString());
			//LogWriter.addLog(String.format("%tF %tT [INFO][%s] %s\n", d, d, tag, obj.toString()));
		}
	}
	
	public void c(Object obj) {
		System.out.printf("\033[35m%s\033[0m\n", obj);
		LogWriter.addLog(obj.toString() + "\n");
	}
	
	public void setTag(String tag){
		this.tag = tag;
	}
}
