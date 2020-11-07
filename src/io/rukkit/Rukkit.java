package io.rukkit;
import io.rukkit.net.*;
import java.io.*;
import io.rukkit.util.*;
import io.rukkit.map.*;
import io.rukkit.plugin.*;

public class Rukkit
{
	public static GameServer game;
	public static final String RUKKIT_VERSION = "0.5.2-dev";
	public static final String RUKKIT_API_VERSION = "0.2.2-dev";
	private static PluginManager PluginManager = new PluginManager();
	private static Logger log = new Logger("Main");
	
	public static PluginManager getCurrentPluginManager(){
		return PluginManager;
	}
	
	public static void main(final String args[]) throws InterruptedException, Exception{
		Long time = System.currentTimeMillis();
		try{
			if(new File("server.properties").isFile() && new File("server.properties").exists()){
				log.i("Loading properties......");
				ServerProperties.readProperties();
			}else{
				log.i("Creating properties......");
				ServerProperties.storageProperties();
			}
			if(!CustomMapLoader.createDir()){
				log.e("Map folder create failed.Stopping...");
			}
			if(ServerProperties.onlineMode){
				new Thread(new Runnable(){public void run(){
							try
							{
								//NetListHelper.startPublish(Integer.parseInt(args[0]));
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}
						}}).start();
			}
			log.i("Game server ID is : " + ServerProperties.UUID);
			log.i("Starting server at port" + Integer.parseInt(args[0]));
			//Start server!
			/*PluginLoader loader = new PluginLoader();
			loader.loadPlugin();*/
			PluginManager.loadPluginInDir();
			(game = new GameServer(Integer.parseInt(args[0]))).action(time);
		}catch(ArrayIndexOutOfBoundsException e){
			if(ServerProperties.onlineMode){
				new Thread(new Runnable(){public void run(){
							try
							{
								//NetListHelper.startPublish(5123);
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}
						}}).start();
			}
			log.i("Starting server at port 5123......");
			/*PluginLoader loader = new PluginLoader();
			loader.loadPlugin();*/
			PluginManager.loadPluginInDir();
			(game = new GameServer(5123)).action(time);
		}
		
	}
}
