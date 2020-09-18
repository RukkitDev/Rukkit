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
		System.out.printf("%tF %tT [INFO][%s] %s\n", d, d, tag, obj.toString());
	}
	
	public void w(Object obj){
		Date d = new Date();
		System.out.printf("\033[33m%tF %tT [WARN][%s] %s\033[0m\n", d, d, tag, obj.toString());
	}
	
	public void e(Object obj){
		Date d = new Date();
		System.out.printf("\033[31m%tF %tT [ERROR][%s] %s\033[0m\n", d, d, tag, obj.toString());
	}
	
	public void d(Object obj){
		if(ServerProperties.isDebug){
			Date d = new Date();
			System.out.printf("\033[32m%tF %tT [DEBUG][%s] %s\033[0m\n", d, d, tag, obj.toString());
		}
	}
	
	public void setTag(String tag){
		this.tag = tag;
	}
}
