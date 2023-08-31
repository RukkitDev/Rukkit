/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.game;
import cn.rukkit.*;
import cn.rukkit.network.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;

public class NetworkPlayer
{
	NetworkPlayerData data;

	public String name = "Player - Empty";
	public String uuid = "null";
	public int verifyCode = 114514;
	public int credits = 4000;
	public int team = 0;
	public int playerIndex;
	public boolean isEmpty = true;
	//public index;
	// Preparing for 1.15
	public int startingUnit;

	private RoomConnection connection = null;

	public int ping = -1;
	public boolean isAdmin = false;

	public boolean isAI = false;

	public boolean isSharingControl = false;

	public boolean isSurrounded = false;
	private NetworkRoom room;

	public NetworkPlayer(RoomConnection connection) {
		this.connection = connection;
		this.room = connection.currectRoom;
		this.isEmpty = false;
	}

	public NetworkPlayer() {
		this.connection = null;
		this.isEmpty = true;
	}
	
	public RoomConnection getConnection() {
		return this.connection;
	}
	
	public NetworkRoom getRoom() {
		return this.room;
	}

	/**
	 * get a extraData as a object, etc..
	 * @param key
	 * @param defaultValue
	 * @param tClass
	 * @return
	 * @param <T>
	 */
	public <T> T getExtraDataAs(String key, T defaultValue, Class<T> tClass) {
		return (T) data.extraData.getOrDefault(key, defaultValue);
	}

	/**
	 * 获取玩家的临时数据
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public Object getTempData(String key, Object defaultValue) {
		return data.tempData.getOrDefault(key, defaultValue);
	}

	/**
	 * 放入临时数据
	 * @param key
	 * @param value
	 */
	public void putTempData(String key, Object value) {
		data.tempData.put(key, value);
	}

	public void clearTempData() {
		data.tempData = new HashMap<String, Object>();
	}

	/**
	 * Get extra data.
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public Object getExtraData(String key, Object defaultValue) {
		return data.extraData.getOrDefault(key, defaultValue);
	}

	/**
	 * Put a data to player's ExtraData.
	 * @param key
	 * @param value
	 */
	public void putExtraData(String key, Object value) {
		data.extraData.put(key, value);
	}

	/**
	 * Save player data.
	 */
	public void savePlayerData() {
		Yaml yaml = new Yaml(new Constructor(NetworkPlayerData.class));
		try {
			FileWriter writer = new FileWriter(Rukkit.getEnvPath() + "/data/player/" + uuid + ".yaml");
			writer.write(yaml.dumpAs(data, null, DumperOptions.FlowStyle.BLOCK));
			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			//This should NEVER HAPPEN!
		} catch (IOException e) {

		}
	}
	
	public void writePlayer(DataOutputStream stream, boolean simpleMode) throws IOException {
		if (simpleMode) {
			stream.writeByte(0);
			stream.writeInt(ping);
			stream.writeBoolean(true);
			stream.writeBoolean(true);
		} else {
			//玩家位置
			stream.writeByte(playerIndex);
			//玩家资金(毫无作用)
			stream.writeInt(credits);
			//玩家队
			stream.writeInt(team);

			stream.writeBoolean(true);

			if(isAdmin){
				stream.writeUTF("[[[" + name + "]]]");
			}else{
				stream.writeUTF(name);
			}
			stream.writeBoolean(true);

			//enc.stream.writeBoolean(true);
			stream.writeInt(ping);
			stream.writeLong(System.currentTimeMillis());

			//是否AI
			stream.writeBoolean(isAI);
			//AI难度
			stream.writeInt(0);

			//玩家队伍
			stream.writeInt(team);
			stream.writeByte(0);

			//分享控制
			stream.writeBoolean(isSharingControl);
			//是否掉线
			stream.writeBoolean(false);

			//是否投降
			stream.writeBoolean(isSurrounded);
			stream.writeBoolean(false);
			stream.writeInt(-9999);

			stream.writeBoolean(false);
			//是否房主
			stream.writeInt(0);

			// 1.15新增
			stream.writeBoolean(false);
			stream.writeBoolean(false);
			stream.writeBoolean(false);
			stream.writeBoolean(false);

			//占个位先
			stream.writeInt(0);
		}
	}
	
	public boolean movePlayer(int index){
		//If index larger then maxPlayer
		if (index > Rukkit.getConfig().maxPlayer) return false;
		PlayerManager playerGroup = room.playerManager;
		if (!playerGroup.get(index).isEmpty) {
			return false;
		}
		this.playerIndex = index;
		playerGroup.remove(this);
		playerGroup.set(index, this);
		return true;
	}

	public boolean moveTeam(int team){
		if(team > 9 || team < 0){
			return false;
		} else {
			this.team = team;
		}
		return true;
	}

	public boolean giveAdmin(int index){
		NetworkPlayer player = room.playerManager.get(index);
		if(index < Rukkit.getConfig().maxPlayer && index >= 0 && !player.isEmpty && this.isAdmin){
			player.isAdmin = true;
			this.isAdmin = false;
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return "NetworkPlayer{" +
				"name='" + name + '\'' +
				", uuid='" + uuid + '\'' +
				", team=" + team +
				", playerIndex=" + playerIndex +
				", ping=" + ping +
				", isAdmin=" + isAdmin +
				", isSharingControl=" + isSharingControl +
				", isSurrounded=" + isSurrounded +
				'}';
	}

	public boolean isNull() {
		return false;
	}

	public static final void initPlayerDataDir() {
		File dataDir = new File(Rukkit.getEnvPath() + "/data");
		if (!dataDir.isDirectory()) {
			dataDir.delete();
			dataDir.mkdir();
		}
		File userDataDir = new File(Rukkit.getEnvPath() + "/data/player");
		if (!userDataDir.isDirectory()) {
			userDataDir.delete();
			userDataDir.mkdir();
		}
	}

	public void loadPlayerData() {
		Logger log = LoggerFactory.getLogger("PlayerData");
		log.info("Load player infomation.");
		Yaml yaml = new Yaml(new Constructor(NetworkPlayerData.class));
		File dataFile = new File(Rukkit.getEnvPath() + "/data/player/" + uuid + ".yaml");
		try {
			if (dataFile.exists()) {
				log.info("Player exists.Loading...");
				data = yaml.load(new FileInputStream(dataFile));
				data.lastUsedName = name;
				data.lastConnectedTime = new Date().toString();
				data.lastConnectedAddress = connection.handler.ctx.channel().remoteAddress().toString();
				Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dataFile), StandardCharsets.UTF_8));
				writer.write(yaml.dumpAs(data, null, DumperOptions.FlowStyle.BLOCK));
				writer.flush();
				writer.close();
			} else {
				log.info("New player.Creating file...");
				dataFile.createNewFile();
				data = new NetworkPlayerData();
				data.uuid = uuid;
				data.lastUsedName = name;
				data.lastConnectedTime = new Date().toString();
				data.lastConnectedAddress = connection.handler.ctx.channel().remoteAddress().toString();
				Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dataFile), StandardCharsets.UTF_8));
				writer.write(yaml.dumpAs(data, null, DumperOptions.FlowStyle.BLOCK));
				writer.flush();
				writer.close();
			}
		} catch (FileNotFoundException ignored) {
			// Never happen!
		} catch (IOException e) {

		}
	}
}
