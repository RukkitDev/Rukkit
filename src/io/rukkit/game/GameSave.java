package io.rukkit.game;
import io.rukkit.net.*;
import java.io.*;
import io.rukkit.util.*;
import io.rukkit.entity.*;
import org.yaml.snakeyaml.parser.*;

public class GameSave
{

	private GameInputStream gameInputStream;

	private GameOutputStream gameOutputStream;

	private Logger log = new Logger("GameSave");

	public GameSave(GameOutputStream s)
	{
		this.gameOutputStream = s;
		//this.saveFile = new File(path);
	}

	public GameSave(GameInputStream s)
	{
		this.gameInputStream = s;
		//this.saveFile = new File(path);
	}

	public GameSave(GameInputStream s, GameOutputStream o)
	{
		this.gameInputStream = s;
		this.gameOutputStream = o;
		//this.saveFile = new File(path);
	}

	public void loadSave(String saveFile)
	{

	}

	public void printSaveData() throws IOException
	{
		if (this.gameInputStream == null)
		{
			throw new NullPointerException("InputStream is null.Is this a output saver?");
		}

		log.d("Header: " + gameInputStream.readString());
		gameInputStream.readInt();
		int version;
		log.d("Save Version: " +  (version = gameInputStream.readInt()));
		//Packet version
		if (version >= 5)
		{
			gameInputStream.readBoolean();
		}

		if (version >= 23)
		{
			DataInputStream in = gameInputStream.getDecodeStream();

			if (version >= 54)
			{
				GameInputStream customIn = new GameInputStream(in);
				//customIn.getDecodeStream();
				log.d(customIn.readString() + customIn.readInt());
				GameInputStream customUnits = new GameInputStream(customIn.getDecodeBytes());
				int basic,maxunit;
				log.d((basic = customUnits.readInt()) + " Units: " + (maxunit = customUnits.readInt()));
				for (int i = basic;i < maxunit;i++)
				{
					log.d("UnitName: " + customUnits.readString());
					log.d("Unit id: " + customUnits.readInt());
					customUnits.readBoolean();
					if (customUnits.readBoolean())
					{
						log.d("Mod Name: " + customUnits.readString());
					}
					customUnits.readLong();
					customUnits.readLong();
				}
			}

			if (version >= 56)
			{
				GameInputStream s = new GameInputStream(in);
				GameInputStream gameSetupStream = new GameInputStream(s.getDecodeBytes());
				if (gameSetupStream.readBoolean())
				{
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

			if (version >= 72 && in.readBoolean())
			{
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

			if (in.readBoolean())
			{
				int unitmin = in.readInt();
				int unitmax = in.readInt();
				for (int i = 0;i < unitmin;i++)
				{
					for (int i2 = 0;i2 < unitmax;i++)
					{
						log.d(in.readByte());
					}
				}
			}

			if (version >= 86)
			{
				log.d(in.readBoolean());
				log.d(in.readBoolean());
				log.d(in.readBoolean());
				log.d(in.readBoolean());
			}

			if (in.readBoolean())
			{
				log.d("A boolean is true.");
				log.d(in.readBoolean());
				log.d(in.readInt());
				log.d(in.readInt());
				log.d(in.readInt());
				log.d(in.readInt());
				log.d(in.readInt());
				log.d(in.readFloat());
				log.d(in.readFloat());
				log.d(in.readFloat());
				log.d(in.readBoolean());
				int step = in.readInt();
				log.d("Step?:" + step);
				if (step > 0)
				{
					int count = in.readInt();
					log.d("unit count");
					for (int i = 0;i < count;i++)
					{
						log.d(in.readUTF());
						log.d(in.readBoolean());
						if (step >= 2)
						{
							log.d(in.readInt());
							log.d(in.readInt());
						}
						log.d(in.readBoolean());
						log.d(in.readInt());
					}
				}
				if (step >= 5)
				{
					log.d(in.readInt());
				}
				if (step >= 6)
				{
					log.d(in.readBoolean());
				}
			}

			//DataInputStream teamStream = new GameInputStream(in).getUnDecodeStream();//new GameInputStream(s.getDecodeBytes())
			log.d("Start of teams:" + in.readShort());
			log.d(in.readInt());
			int maxTeamCount = 8;
			if (version >= 49)
			{
				maxTeamCount = in.readInt();
				log.d("TeamCount:" + maxTeamCount);
			}
			for (int i = 0;i < maxTeamCount;i++)
			{
				boolean isAI = in.readBoolean();
				boolean isNetPlayer = false;
				if (version >= 7)
				{
					isNetPlayer = in.readBoolean();
				}
				log.d("Replace with AI:" + isAI);
				log.d("Replace with netPlayer:" + isNetPlayer);
				log.d("GetOriginalTeam By Client:" + null);
				if (in.readBoolean())
				{
					log.d(String.format("===== Slot %d =====", in.readByte()));
					int credits = in.readInt();
					int team = in.readInt();
					String name = "Unnamed";
					if (in.readBoolean())
					{
						name = in.readUTF();
					}
					log.d(String.format("Name: %s | Credit: %d | Team: %d", name, credits, team));
					log.d(String.format("bool: %b", in.readBoolean()));

					int ping = in.readInt();
					long time = in.readLong();
					log.d("Ping: " + ping + " TimeStamp: " + time);

					log.d(in.readBoolean());
					log.d(in.readInt());

					log.d("PlayerIndex: " + in.readInt());
					in.readByte();

					log.d("isSharingControl: " + in.readBoolean());
					log.d("is?: " + in.readBoolean());

					log.d(in.readBoolean());
					log.d(in.readBoolean());
					log.d(in.readInt());

					if (in.readBoolean())
					{
						log.d(in.readUTF());
					}
					log.d(in.readInt());
				}
			}
			int size = in.readInt();
			log.d("Total Units:" + size);
			for (int i = 0;i < size;i++)
			{
				int type = in.readByte();
				if (type == 1)
				{
					log.d("TYPE=InternalUnit(enum=" + InternalUnit.units[in.readInt()]);
				}
				if (type == 2)
				{
					log.d(String.format("TYPE=InternalUnit(%b)", in.readByte()));
				}
				if (type == 3)
				{
					log.d(String.format("TYPE=CustomUnit(name=%s", in.readUTF()));
				}
				log.d("UnitId: " + in.readLong());
			}

			//log.d(in.readLong());
			if (version >= 3)
			{
				log.d(in.readLong());
			}

			if (version >= 24)
			{
				log.d(in.readInt());
				log.d(in.readInt());
			}

			if (version >= 4)
			{
				int sizep = in.readInt();
				log.d("Size: " + sizep);
				for (int i = 0;i < sizep;i++)
				{
					log.d(in.readFloat());
					log.d(in.readLong());
					int size2 = in.readInt();
					log.d("size2: " + size2);
					for (int i2 = 0;i2 < size2;i2++)
					{
						log.d("UnitID: " + in.readLong());
					}
					log.d(in.readByte());
				}
				log.d(in.readByte());
				if (in.readByte() > 0)
				{
					log.d("True: " + in.readInt());
				}
			}


			if (version >= 57)
			{
				log.d("Read stats");
				log.d(in.readUTF());
				log.d(in.readInt());
				in.readByte();
				int count = in.readInt();
				log.d("Stat count:" + count);
				for (int i = 0;i < count;i++)
				{
					in.readByte();
					log.d("Mark: " + in.readShort());
					log.d(in.readInt());
					log.d(in.readInt());
					log.d(in.readInt());
					log.d(in.readInt());
					log.d(in.readInt());
					log.d(in.readInt());
					log.d(in.readInt());
					log.d(in.readInt());
					log.d(in.readInt());
					log.d(in.readInt());
					log.d(in.readLong());
				}
			}

			if (version >= 7)
			{
				for (int i = 0;i < maxTeamCount;i++)
				{
					log.d(String.format("===== Slot %d =====", in.readByte()));
					int credits = in.readInt();
					int team = in.readInt();
					String name = "Unnamed";
					if (in.readBoolean())
					{
						name = in.readUTF();
					}
					log.d(String.format("Name: %s | Credit: %d | Team: %d", name, credits, team));
					log.d(String.format("bool: %b", in.readBoolean()));

					int ping = in.readInt();
					long time = in.readLong();
					log.d("Ping: " + ping + " TimeStamp: " + time);

					log.d(in.readBoolean());
					log.d(in.readInt());

					log.d("PlayerIndex: " + in.readInt());
					in.readByte();

					log.d("isSharingControl: " + in.readBoolean());
					log.d("is?: " + in.readBoolean());

					log.d(in.readBoolean());
					log.d(in.readBoolean());
					log.d(in.readInt());

					if (in.readBoolean())
					{
						log.d(in.readUTF());
					}
					log.d(in.readInt());

					byte readByte = in.readByte();
					log.d(readByte);
					log.d(in.readBoolean());
					if (readByte > 0)
					{
						log.d(in.readByte());
					}

					if (in.readBoolean())
					{
						if (in.readBoolean())
						{
							log.d("TRUE");
							int width, height;
							log.d("MAP Width:" + (width = in.readInt()));
							log.d("MAP Height:" + (height = in.readInt()));
							for (int x = 0;x < width;x++)
							{
								for (int y = 0;y < height;y++)
								{
									in.readByte();
									//log.d(String.format("(%d, %d) %s", x, y, in.readByte()));
								}
							}
						}
					}

					if (readByte >= 2)
					{
						byte res = in.readByte();
						if (res != -1) {
							log.d("Custom resources detected.(res=");
							short total = in.readShort();
							log.d("Total custom resources:" + total);
							for (int i2 = 0;i2 < total;i2++) {
								log.d("ResourcesName: " + in.readUTF() + "=" + in.readDouble());
							}
						} else {
							log.d("Cannot find custom res.(=" + res);
						}
					}
					/*for (int i3 = 0; i3< 7;i3 ++) {
						log.d(in.readByte());
					}
					short s;
					while((s = in.readShort()) != 12345) {
						log.d("write" + s);
					}*/
					//log.d(in.readShort());
					/*if (readByte >= 3) {
						if (in.readBoolean())
						{
							log.d(in.readUTF());
						}
					}
					
					if (readByte >= 4) {
						log.d(in.readBoolean());
					}*/
				}
			}
			//log.d(in.readShort());
		}
	}

}
