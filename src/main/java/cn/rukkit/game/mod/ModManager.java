/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.game.mod;

import cn.rukkit.*;
import cn.rukkit.game.mod.Mod.*;
import com.alibaba.fastjson2.*;
import java.io.*;
import java.util.*;
import org.slf4j.*;

public class ModManager 
{
	private Logger log = LoggerFactory.getLogger(ModLoader.class);;
	private static String MOD_PATH = Rukkit.getEnvPath() + "/mods";
	private HashMap<String, Mod> loadedMods = new HashMap<String, Mod>();
	//private HashMap<String, ArrayList<ModUnit>> loadedMods = new HashMap<String, ArrayList<ModUnit>>();

	public ModManager () {
		if (!createModDir()) {
			log.error("ModManager::Mod Exception.Server will shutdown...");
			Rukkit.shutdown("Mod System fatal error.");
		}
	}

	private boolean createModDir() {
		File f = new File(MOD_PATH);
		if (f.exists() && f.isDirectory()) {
			return true;
		} else {
			//log.i("Mod folder is not exist.creating...");
			f.delete();
			f.mkdir();
			return true;
		}
	}

	public void loadAllModsInDir() {
		File dir = new File(MOD_PATH);
		log.info("" + dir);
		for (File f: dir.listFiles()) {
			if (f.getName().startsWith("mod_")) {
				String modname = f.getName().substring(4);
				try
				{
					loadMod(modname, f);
				}
				catch (IOException e)
				{
					log.error("Load Mod: " + modname + " failed.", e);
					//log.error(e);
				}
			}
		}
	}

	public Mod getModByString(String jsondata) {
		ArrayList<ModUnit> li = new ArrayList<ModUnit>();
		li = (ArrayList<ModUnit>) JSON.parseArray(jsondata, ModUnit.class);
		return new Mod(li);
	}

	public void loadMod(String modname, File f) throws FileNotFoundException, IOException {
		log.info("Loading mod: " + modname);
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
		ArrayList<ModUnit> li = new ArrayList<ModUnit>();
		StringBuffer sbuf = new StringBuffer();
		String b = null;
		while ((b = reader.readLine()) != null)
		{
			sbuf.append(b);
		}
		li = (ArrayList<ModUnit>) JSON.parseArray(sbuf.toString(), ModUnit.class);
		Mod mod = new Mod(li);
        //mod.setEnabled(true);
		loadedMods.put(modname, mod);
		reader.close();
	}

	public void loadModInDir(String modname) throws IOException {
		loadMod(modname, new File(MOD_PATH + "/mod_" + modname + ".json"));
	}

	public void loadMod(String modname, Mod mod) {
		loadedMods.put(modname, mod);
	}

	public void enableMod(String modname) {
		try{
			loadedMods.getOrDefault(modname, null).setEnabled(true);
		} catch (NullPointerException e) {
			log.error(String.format("Mod '%s' is not exist.", modname), e);
		}
	}

	public void disableMod(String modname) {
		try{
			loadedMods.getOrDefault(modname, null).setEnabled(false);
		} catch (NullPointerException e) {
			log.error(String.format("Mod '%s' is not exist.", modname), e);
		}
	}

	public void disableAllMods() {
		for (Map.Entry<String, Mod> mod: loadedMods.entrySet()) {
			((Mod) mod.getValue()).setEnabled(false);
		}
	}

	public void enableAllMods() {
		for (Map.Entry<String, Mod> mod: loadedMods.entrySet()) {
			((Mod) mod.getValue()).setEnabled(true);
		}
	}

	public HashMap<String, Mod> getLoadedModsMap() {
		return loadedMods;
	}

	public Mod fetchMod(String modname){
		return loadedMods.getOrDefault(modname, null);
	}

	public ArrayList<ModUnit> fetchAllEnabledModUnits() {
		ArrayList<ModUnit> list = new ArrayList<ModUnit>();
		for (Map.Entry<String, Mod> entry: loadedMods.entrySet()) {
			if (((Mod)entry.getValue()).isEnabled()) {
				list.addAll(((Mod)entry.getValue()).getUnitList());
			}
		}
		/*for(ModUnit u: list) {
			log.debug(String.format("Unit '%s' from '%s' (%d)", u.getModName(), u.getUnitName(), u.getUnitId()));
		}*/
		return list;
	}

	public void loadInternalMod() throws IOException {
		if (fetchMod("default") != null) {
			log.warn("Deafult mod already loaded.Ignoring...");
			return;
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(ModManager.this.getClass().getClassLoader().getResourceAsStream("mod_default.json")));
		ArrayList<ModUnit> li = new ArrayList<ModUnit>();
		StringBuffer sbuf = new StringBuffer();
		String b = null;
		while ((b = reader.readLine()) != null)
		{
			sbuf.append(b);
		}
		li = (ArrayList<ModUnit>) JSON.parseArray(sbuf.toString(), Mod.ModUnit.class);
		/*for(ModUnit u: li) {
		 log.d(u.getModName());
		 }*/
        Mod mod = new Mod(li);
        mod.setEnabled(true);
		loadMod("default", mod);
		//return li;
	}
}
