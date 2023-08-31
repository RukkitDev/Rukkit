package cn.rukkit.game;

import java.util.HashMap;

public class NetworkPlayerData {
    public String lastUsedName;
    public String uuid;
    public String lastConnectedTime;

    public String lastConnectedAddress;
    public HashMap<String, Object> extraData = new HashMap<String, Object>();
    public HashMap<String, Object> tempData = new HashMap<String, Object>();
}
