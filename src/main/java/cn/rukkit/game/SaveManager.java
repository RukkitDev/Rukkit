/*
 *  All Rights Reserved.
 *  FileName: SaveManager.java
 *  @author: wtbdev
 *  @date: 2022/1/30 下午4:37
 */

package cn.rukkit.game;
import cn.rukkit.Rukkit;
import cn.rukkit.game.SaveManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cn.rukkit.network.packet.Packet;
import java.io.InputStream;

public class SaveManager {
    
    public SaveData defaultSave;
    public SaveData lastSave;
    private Logger log = LoggerFactory.getLogger(SaveManager.class);
    
    public SaveManager() {
        log.info("SaveManager::init");
        try {
            loadDefaultSave();
        } catch (IOException e) {
            log.error("Default save load failed.Sync system is unavailable.", e);
            Rukkit.getConfig().syncEnabled = false;
        }
    }
    
    public SaveData getDeafultSave() {
        return defaultSave;
    }
    
    public SaveData getLastSave() {
        return lastSave;
    }
    
    public void sendDefaultSaveToAll(boolean isPullSave) throws IOException {
        Rukkit.getConnectionManager().broadcast(Packet.sendSave(defaultSave.arr, isPullSave));
    }
    
    public void sendLastSaveToAll(boolean isPullSave) throws IOException {
        if (lastSave != null) {
            Rukkit.getConnectionManager().broadcast(Packet.sendSave(lastSave.arr,isPullSave));
        } else {
            log.error("lastSave is NULL!Ignoring sendLastSaveToAll.");
        }
    }
    
    public void setLastSave(SaveData save) {
        lastSave = save;
    }
    
    private void loadDefaultSave() throws IOException {
        InputStream in = getClass().getClassLoader().getResourceAsStream("defaultSave");
        byte[] data = new byte[in.available()];
        in.read(data);
        in.close();
        SaveData save = new SaveData();
        save.arr = data;
        save.time = 0;
        defaultSave = save;
    }
    
    public void dumpLastSave(String filename) throws FileNotFoundException, IOException {
        File f = new File(Rukkit.getEnvPath() + "/" + filename);
        if (!f.exists()) f.createNewFile();
        FileOutputStream out = new FileOutputStream(f);
        if (lastSave != null) {
            out.write(lastSave.arr);
            out.flush();
            out.close();
        } else {
            log.error("lastSave is NULL!Ignore dump request.");
            //throw new NullPointerException();
        }
    }
    
    public SaveData readSaveFromFile(String filename) throws FileNotFoundException, IOException {
        FileInputStream in = new FileInputStream(filename);
        byte[] data = new byte[in.available()];
        in.read(data);
        in.close();
        SaveData save = new SaveData();
        save.arr = data;
        save.time = 0;
        return save;
    }
}
