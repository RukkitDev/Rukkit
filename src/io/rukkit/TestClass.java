package io.rukkit;

import java.io.*;
import io.rukkit.plugin.*;

public class TestClass
{
	public static void main(String args[]) throws IOException, ClassNotFoundException{
		//new PluginLoader().loadPlugin();
		new PluginManager().loadPluginInDir();
	}
}
