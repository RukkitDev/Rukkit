/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.game;
import cn.rukkit.Rukkit;
import cn.rukkit.game.SaveManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import cn.rukkit.network.NetworkRoom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cn.rukkit.network.packet.Packet;
import java.io.InputStream;

public class SaveManager {

    public SaveData lastSave;
    public NetworkRoom currentRoom;
    private Logger log;
    
    public SaveManager(NetworkRoom room) {
        currentRoom = room;
        log = LoggerFactory.getLogger("SaveManager Room #" + currentRoom.roomId);
    }
    
    public SaveData getDeafultSave() {
        return Rukkit.getDefaultSave();
    }
    
    public SaveData getLastSave() {
        return lastSave;
    }
    
    public void sendDefaultSaveToAll(boolean isPullSave) throws IOException {
        currentRoom.broadcast(Packet.sendSave(currentRoom, getDeafultSave().arr, isPullSave));
    }
    
    public void sendLastSaveToAll(boolean isPullSave) throws IOException {
        if (lastSave != null) {
            currentRoom.broadcast(Packet.sendSave(currentRoom, lastSave.arr,isPullSave));
        } else {
            log.error("lastSave is NULL!Ignoring sendLastSaveToAll.");
        }
    }
    
    public void setLastSave(SaveData save) {
        lastSave = save;
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
    
    public static SaveData readSaveFromFile(String filename) throws FileNotFoundException, IOException {
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
