package io.rukkit.map;
import io.rukkit.*;
import io.rukkit.util.*;
import java.io.*;
import java.util.*;

public class CustomMapLoader
{
	private static String MAP_FOLDER = Rukkit.getEnvPath() + "/maps";
	private static final Logger log = new Logger("CustomMapLoader");
	
	public static boolean createDir(){
		File folder = new File(MAP_FOLDER);
		folder.mkdir();
		if(!folder.isDirectory()){
			log.w("Not a dir.Change a another dir...");
			MAP_FOLDER = MAP_FOLDER + "_rukkit";
			folder = new File(MAP_FOLDER);
			folder.mkdir();
			if(!folder.exists() || !folder.isDirectory()){
				log.e("Load failed.Stop working.");
				return false;
			}
		}
		return true;
	}
	
	public static ArrayList<String> getMapList(){
		File folder = new File(MAP_FOLDER);
		ArrayList<String> list = new ArrayList<String>();
		for(String f: folder.list()){
			String[] n = f.split("\\.");
			if(n[n.length - 1].equals("tmx")){
				list.add(f);
			}
		}
		return list;
	}
	
	public static ArrayList<String> getMapNameList(){
		File folder = new File(MAP_FOLDER);
		ArrayList<String> list = new ArrayList<String>();
		for(String f: folder.list()){
			String[] n = f.split("\\.");
			if(n[n.length - 1].equals("tmx")){
				StringBuffer sbf = new StringBuffer();
				for(int i = 0;i < n.length -1;i++){
					sbf.append(n[i]);
				}
				list.add(sbf.toString());
			}
		}
		return list;
	}
	
	public static FileInputStream getStreamById(int id) throws FileNotFoundException{
		return new FileInputStream(getMapList().get(id));
	}
	
	public static FileInputStream getStreamByName(String name) throws FileNotFoundException{
		return new FileInputStream(MAP_FOLDER + "/" + name);
	}
}
