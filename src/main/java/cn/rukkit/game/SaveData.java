package cn.rukkit.game;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cn.rukkit.network.GameInputStream;
import java.io.IOException;
import cn.rukkit.network.GameOutputStream;
import cn.rukkit.Rukkit;
import cn.rukkit.game.map.CustomMapLoader;

public class SaveData {
    
    public int sendBy;
	public byte[] arr;
    public int time;
	private Logger log = LoggerFactory.getLogger(SaveData.class);
    
    public SaveData() {
        
    }
    
    public void loadSave() throws IOException {
		GameInputStream in = new GameInputStream(arr);
		log.debug("{}, {}", in.readString(), in.readInt());
		int version;
        log.debug("Save Header: {}", in.readString());
		in.readInt();
		log.debug("Save Version: {}", version = in.readInt());
		if (version >= 5) {
			log.debug("" + in.readBoolean());
		}
		if (version >= 23) {
			in.startBlock();

			if (version >= 54) {
				log.debug("{}, {}", in.readString(), in.readInt());
				log.debug("{}, {}", in.readString(), in.readInt());
				int basic, maxunit;
				log.debug("Basic={}, MaxUnit={}", basic = in.readInt(), maxunit = in.readInt());
				for (int i = basic;i <= maxunit;i++) {
					log.debug("Unit: {} (id={})", in.readString(), in.readInt());
					in.readBoolean();
					if (in.readBoolean()) {
						log.debug("From MOD:{}", in.readString());
					}
					in.readLong();in.readLong();
				}
			}

			if (version >= 56) {
				/*log.debug("{}, {}", in.readString(), in.readInt());
				if (in.readBoolean()) {
					in.readInt();
					log.debug("{}, {}, {}, {}, {}, {}, {}, {}, {}"
							  ,in.readInt(), in.readInt(), in.readBoolean(),
							  in.readInt(), in.readInt(), in.readFloat(),
							  in.readBoolean(), in.readBoolean(), in.readBoolean());
				}*/
				in.getDecodeBytes();
			}

			log.debug("MapPath={}", in.readString());

			if (version >= 73 && in.readBoolean()) {
				in.readStreamBytes();
			}
			
			int tickTime;
			float mapX, mapY;
			
			log.debug("{}, {}, {}, {}",
					  tickTime = in.readInt(), mapY = in.readFloat(), mapX = in.readFloat(), in.readFloat());
					  
			this.time = tickTime;
			
			log.debug("{}, {}",
					  in.readInt(), in.readInt());

			log.debug("Setup ended(Mark={}).", in.readShort());

			/*if (in.readBoolean()) {
				int x = in.readInt();
				int y = in.readInt();
				for (int i=0;i<x;i++) {
					for (int j=0;j<y;i++) {
						in.readByte();
					}
				}
			}

			if (version >= 86) {
				log.debug("{}, {}, {}, {}",
						  in.readBoolean(), in.readBoolean(), in.readBoolean(), in.readBoolean());
			}

			if (in.readBoolean()) {

			}*/
		}
    }
	
	public void injectMapToSave() throws IOException {
		GameInputStream in = new GameInputStream(arr);
		GameOutputStream out = new GameOutputStream();
		log.debug("{}, {}", in.readString(), in.readInt());
		out.startBlock("gameSave", false);
		int version;
        //log.debug("Save Header: {}", in.readString());
		out.writeString(in.readString());
		out.writeInt(in.readInt());
		log.debug("Save Version: {}", version = in.readInt());
		out.writeInt(version);
		if (version >= 5) {
			out.writeBoolean(in.readBoolean());
		}
		if (version >= 23) {
			in.startBlock();
			out.startBlock("saveCompression", true);

			if (version >= 54) {
				out.writeString(in.readString());
				out.writeInt(in.readInt());
				out.writeString(in.readString());
				out.writeInt(in.readInt());
				int basic, maxunit;
				log.debug("Basic={}, MaxUnit={}", basic = in.readInt(), maxunit = in.readInt());
				out.writeInt(basic);
				out.writeInt(maxunit);
				for (int i = basic;i <= maxunit;i++) {
					//log.debug("Unit: {} (id={})", in.readString(), in.readInt());
					String name = in.readString();
					log.debug("U: {}", name);
					out.writeString(name);
					out.writeInt(in.readInt());
					out.writeBoolean(in.readBoolean());
					if (in.readBoolean()) {
						out.writeBoolean(true);
						//log.debug("From MOD:{}", in.readString());
						out.writeString(in.readString());
					}
					out.writeLong(in.readLong());
					out.writeLong(in.readLong());
				}
			}

			if (version >= 56) {
				/*log.debug("{}, {}", in.readString(), in.readInt());
				 if (in.readBoolean()) {
				 in.readInt();
				 log.debug("{}, {}, {}, {}, {}, {}, {}, {}, {}"
				 ,in.readInt(), in.readInt(), in.readBoolean(),
				 in.readInt(), in.readInt(), in.readFloat(),
				 in.readBoolean(), in.readBoolean(), in.readBoolean());
				 }*/
				out.stream.write(in.getDecodeBytes());
			}
			
			String mapPath = in.readString();
			log.debug("MapPath={}", mapPath);
			
			
			if (version >= 73 && in.readBoolean()) {
				in.readStreamBytes();
			}
			
			if (Rukkit.getRoundConfig().mapType == 1) {
				mapPath = Rukkit.getRoundConfig().mapName + ".tmx";
				out.writeString(mapPath);
				out.writeBoolean(true);
				out.writeFile(CustomMapLoader.getStreamByName(Rukkit.getRoundConfig().mapName + ".tmx"));
			} else {
				out.writeString(mapPath);
				out.writeBoolean(false);
			}
			
			int tickTime;
			float mapX, mapY;

			log.debug("{}, {}, {}, {}",
					  tickTime = in.readInt(), mapY = in.readFloat(), mapX = in.readFloat(), in.readFloat());

			this.time = tickTime;

			log.debug("{}, {}",
					  in.readInt(), in.readInt());

			log.debug("Setup ended(Mark={}).", in.readShort());
			
			byte[] cArr = new byte[in.stream.available()];
			in.stream.read(cArr);
			out.stream.write(cArr);
			in.endBlock();
			out.endBlock();
			cArr = new byte[in.stream.available()];
			in.stream.read(cArr);
			out.stream.write(cArr);
			out.endBlock();
			out.stream.flush();
			out.buffer.flush();
			byte[] resultArr = out.buffer.toByteArray();
			arr = resultArr;
		}
	}
}