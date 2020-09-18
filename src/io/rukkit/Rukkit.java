package io.rukkit;
import io.rukkit.net.*;
import java.io.*;
import io.rukkit.util.*;
import io.rukkit.map.*;

public class Rukkit
{
	public static GameServer game;
	private static Logger log = new Logger("Main");
	
	public static void main(final String args[]) throws InterruptedException, Exception{
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
								NetListHelper.startPublish(Integer.parseInt(args[0]));
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
			(game = new GameServer(Integer.parseInt(args[0]))).action();
		}catch(ArrayIndexOutOfBoundsException e){
			if(ServerProperties.onlineMode){
				new Thread(new Runnable(){public void run(){
							try
							{
								NetListHelper.startPublish(5123);
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}
						}}).start();
			}
			log.i("Starting server at port 5123......");
			(game = new GameServer(5123)).action();
		}
		
	}
}
