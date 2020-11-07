package io.rukkit.game;
import io.rukkit.net.*;
import java.io.*;
import io.rukkit.util.*;

public class GameSave
{

	private GameInputStream gameInputStream;

	private GameOutputStream gameOutputStream;
	
	private Logger log = new Logger("GameSave");
	
	public GameSave(GameOutputStream s){
		this.gameOutputStream = s;
		//this.saveFile = new File(path);
	}
	
	public GameSave(GameInputStream s){
		this.gameInputStream = s;
		//this.saveFile = new File(path);
	}
	
	public GameSave(GameInputStream s,GameOutputStream o){
		this.gameInputStream = s;
		this.gameOutputStream = o;
		//this.saveFile = new File(path);
	}
	
	public void loadSave(String saveFile){
		
	}
	
	public void printSaveData() throws IOException{
		if(this.gameInputStream == null){
			throw new NullPointerException("InputStream is null.Is this a output saver?");
		}
		
		log.d("Header: " + gameInputStream.readString());
		gameInputStream.readInt();
		int version;
		log.d("Save Version: " +  (version = gameInputStream.readInt()) );
		//Packet version
		if(version >= 5){
			gameInputStream.readBoolean();
		}
		
		if(version >= 23){
			DataInputStream in = gameInputStream.getDecodeStream();
			
			if(version >= 54){
				GameInputStream customIn = new GameInputStream(in);
				//customIn.getDecodeStream();
				log.d(customIn.readString() + customIn.readInt());
				GameInputStream customUnits = new GameInputStream(customIn.getDecodeBytes());
				int basic,maxunit;
				log.d((basic = customUnits.readInt()) + " Units: " + (maxunit = customUnits.readInt()));
				for(int i = basic;i < maxunit;i++){
					log.d("UnitName: " + customUnits.readString());
					log.d("Unit id: " + customUnits.readInt());
					customUnits.readBoolean();
					if(customUnits.readBoolean()){
						log.d("Mod Name: " + customUnits.readString());
					}
					customUnits.readLong();
					customUnits.readLong();
				}
			}
			
			if(version >= 56){
				GameInputStream s = new GameInputStream(in);
				GameInputStream gameSetupStream = new GameInputStream(s.getDecodeBytes());
				if(gameSetupStream.readBoolean()){
					gameSetupStream.readInt();
					log.d(gameSetupStream.readInt());
					log.d(gameSetupStream.readInt());
					log.d(gameSetupStream.readBoolean());
					log.d(gameSetupStream.readInt());
					log.d(gameSetupStream.readInt());
					log.d(gameSetupStream.readFloat());
					log.d(gameSetupStream.readBoolean());
					log.d(gameSetupStream.readBoolean());
					log.d(gameSetupStream.readBoolean());
				}
			}
			
			log.d("Map path: " + in.readUTF());
			
			if(version >= 72 && in.readBoolean()){
				log.d("Got the stream");
				GameInputStream s2 = new GameInputStream(in);
				GameInputStream mapStream = new GameInputStream(s2.readStreamBytes());
			}
			
			log.d(in.readInt());
			log.d(in.readFloat());
			log.d(in.readFloat());
			log.d(in.readFloat());
			
			log.d(in.readInt());
			in.readInt();
			
			log.d("Setup ended: " + in.readShort());
			
			if(in.readBoolean()){
				int unitmin = in.readInt();
				int unitmax = in.readInt();
				for(int i = 0;i< unitmin;i++){
					for(int i2 = 0;i2< unitmax;i++){
						log.d(in.readByte());
					}
				}
			}
			//DataInputStream teamStream = new GameInputStream(in).getUnDecodeStream();//new GameInputStream(s.getDecodeBytes())
			log.d(in.readShort());
			log.d(in.readInt());
			log.d(in.readInt());
		}
		
	}
	
}
