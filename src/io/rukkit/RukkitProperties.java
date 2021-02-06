package io.rukkit;
import java.io.*;
import org.yaml.snakeyaml.*;

public class RukkitProperties
{
	public static RukkitProperties readProfile() {
		File f = new File(Rukkit.getEnvPath() + "/rukkit.yml");
		if (f.isDirectory() || !f.exists()) {
			f.delete();
			try
			{
				f.createNewFile();
				Yaml y = new Yaml();
				RukkitProperties conf = new RukkitProperties();
				y.dump(conf, new FileWriter(f));
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		return new RukkitProperties();
	}
	
	public int serverTick = 10;;

	public void setServerTick(int serverTick)
	{
		this.serverTick = serverTick;
	}

	public int getServerTick()
	{
		return serverTick;
	}
	
}
