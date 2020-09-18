package io.rukkit;
import io.rukkit.game.*;
import io.rukkit.net.*;
import java.io.*;

public class SaveReader
{
	public static void main(String args[]) throws FileNotFoundException, IOException{
		FileInputStream in = new FileInputStream(args[0]);
		byte[] b = new byte[in.available()];
		in.read(b);
		new GameSave(new GameInputStream(b)).printSaveData();
	}
}
