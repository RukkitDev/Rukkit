package io.rukkit.mod;
import java.io.*;
import io.rukkit.util.*;
import java.util.*;
import io.rukkit.entity.*;
import com.alibaba.fastjson.*;
import io.rukkit.*;

public class ModManager
{
	private Logger log = new Logger("ModManager");
	private static String MOD_PATH = Rukkit.getEnvPath() + "/mods";
	private HashMap<String, Mod> loadedMods = new HashMap<String, Mod>();
	//private HashMap<String, ArrayList<ModUnit>> loadedMods = new HashMap<String, ArrayList<ModUnit>>();
	
	public ModManager () {
		if (!createModDir()) {
			log.e("Mod System Exception.Server will shutdown...");
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
		log.i("" + dir);
		for (File f: dir.listFiles()) {
			if (f.getName().startsWith("mod_")) {
				String modname = f.getName().substring(4);
				try
				{
					loadMod(modname, f);
				}
				catch (IOException e)
				{
					log.e("Load Mod: " + modname + " failed.");
					log.e(e);
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
		log.i("Loading mod: " + modname);
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
		loadedMods.put(modname, mod);
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
			log.e(String.format("Mod '%s' is not exist.", modname));
		}
	}
	
	public void disableMod(String modname) {
		try{
			loadedMods.getOrDefault(modname, null).setEnabled(false);
		} catch (NullPointerException e) {
			log.e(String.format("Mod '%s' is not exist.", modname));
		}
	}
	
	public void disableAllMods() {
		for (Map.Entry mod: loadedMods.entrySet()) {
			((Mod) mod.getValue()).setEnabled(false);
		}
	}
	
	public void enableAllMods() {
		for (Map.Entry mod: loadedMods.entrySet()) {
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
		for (Map.Entry entry: loadedMods.entrySet()) {
			if (((Mod)entry.getValue()).isEnabled()) {
				list.addAll(((Mod)entry.getValue()).getUnitList());
			}
		}
		for(ModUnit u: list) {
			log.d(String.format("Unit '%s' from '%s' (%d)", u.getModName(), u.getUnitName(), u.getUnitId()));
		}
		return list;
	}
	
	public ArrayList<ModUnit> loadInternalMod() throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(ModManager.this.getClass().getClassLoader().getResourceAsStream("mod_default.json")));
		ArrayList<ModUnit> li = new ArrayList<ModUnit>();
		StringBuffer sbuf = new StringBuffer();
		String b = null;
		while ((b = reader.readLine()) != null)
		{
			sbuf.append(b);
		}
		li = (ArrayList<ModUnit>) JSON.parseArray(sbuf.toString(), ModUnit.class);
		/*for(ModUnit u: li) {
			log.d(u.getModName());
		}*/
		return li;
	}
}
