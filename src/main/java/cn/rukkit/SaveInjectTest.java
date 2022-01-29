package cn.rukkit;

import cn.rukkit.game.SaveData;
import cn.rukkit.network.GameOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class SaveInjectTest {
    static Logger log = LoggerFactory.getLogger(SaveInjectTest.class);
    public static void main(String[] args) throws FileNotFoundException, IOException {
        log.info("Save from defaultSave.");
        SaveData data = new SaveData();
        FileInputStream in = new FileInputStream(Rukkit.getEnvPath() + "/defaultSave");
        byte[] b = new byte[in.available()];
        in.read(b);

        data.arr = b;
        GameOutputStream out = new GameOutputStream();
        data.writeInjectedData(out);
        out.createPacket(180);
        File f = new File(Rukkit.getEnvPath() + "/out");
        if (!f.exists()) f.createNewFile();
        FileOutputStream outputStream = new FileOutputStream(f);
        outputStream.write(out.buffer.toByteArray());
    }
}
