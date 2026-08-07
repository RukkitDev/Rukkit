/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.core.packet;

import cn.rukkit.Rukkit;
import cn.rukkit.config.RoundConfig;
import cn.rukkit.game.CheckSumList;
import cn.rukkit.game.GameActions;
import cn.rukkit.game.PingType;
import cn.rukkit.game.SaveData;
import cn.rukkit.game.map.CustomMapLoader;
import cn.rukkit.game.mod.Mod.ModUnit;
import cn.rukkit.game.unit.InternalUnit;
import cn.rukkit.network.NetworkRoom;
import cn.rukkit.network.command.GameCommand;
import cn.rukkit.network.io.GameOutputStream;
import cn.rukkit.network.room.ServerRoom;
import cn.rukkit.util.GameUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

/**
 * Packet builders for the existing protocol.
 *
 * <p>This class is the first compatibility step in the network migration. It
 * deliberately keeps the old room and command parameter types so the old
 * runtime can be moved to the new packet package without changing wire
 * behavior. The room overloads can be changed to {@code ServerRoom} after the
 * room model migration is complete.</p>
 */
public final class UniversalPacket {
    private UniversalPacket() {
    }

    // Compatibility aliases for callers that used constants from the old Packet class.
    public static final int PACKET_REGISTER_CONNECTION = PacketType.REGISTER_CONNECTION;
    public static final int PACKET_TEAM_LIST = PacketType.TEAM_LIST;
    public static final int PACKET_HEART_BEAT = PacketType.HEART_BEAT;
    public static final int PACKET_SEND_CHAT = PacketType.SEND_CHAT;
    public static final int PACKET_SERVER_INFO = PacketType.SERVER_INFO;
    public static final int PACKET_START_GAME = PacketType.START_GAME;
    public static final int PACKET_QUESTION = PacketType.QUESTION;
    public static final int PACKET_QUESTION_RESPONCE = PacketType.QUESTION_RESPONCE;
    public static final int PACKET_PREREGISTER_CONNECTION = PacketType.PREREGISTER_CONNECTION;
    public static final int PACKET_HEART_BEAT_RESPONSE = PacketType.HEART_BEAT_RESPONSE;
    public static final int PACKET_ADD_CHAT = PacketType.ADD_CHAT;
    public static final int PACKET_PLAYER_INFO = PacketType.PLAYER_INFO;
    public static final int PACKET_DISCONNECT = PacketType.DISCONNECT;
    public static final int PACKET_RANDY = PacketType.READY;
    public static final int PACKET_ADD_GAMECOMMAND = PacketType.ADD_GAMECOMMAND;
    public static final int PACKET_TICK = PacketType.TICK;
    public static final int PACKET_SYNC_CHECKSUM = PacketType.SYNC_CHECKSUM;
    public static final int PACKET_SYNC_CHECKSUM_RESPONCE = PacketType.SYNC_CHECKSUM_RESPONCE;
    public static final int PACKET_SYNC = PacketType.SYNC;

    public static Packet chat(String from, String msg, int team) throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeString(msg);
        output.writeByte(3);
        output.writeBoolean(true);
        output.writeString(from);
        output.writeInt(team);
        output.writeInt(team);
        return output.createPacket(PacketType.SEND_CHAT);
    }

    public static Packet ping() throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeLong(new Random().nextLong());
        output.writeByte(0);
        return output.createPacket(PacketType.HEART_BEAT);
    }

    public static Packet preRegister() throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeString("cn.rukkit");
        output.writeInt(1);
        output.writeInt(176);
        output.writeInt(176);
        output.writeString("cn.rukkit");
        output.writeString(Rukkit.getConfig().UUID);
        output.writeInt(114514);
        output.writeInt(176);
        return output.createPacket(PacketType.REGISTER_CONNECTION);
    }

    public static Packet gameCommand(int tick, GameCommand command) throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeInt(tick);
        output.writeInt(1);
        output.startBlock("c", false);
        output.write(command.arr);
        output.endBlock();
        return output.createPacket(PacketType.TICK);
    }

    public static Packet emptyCommand(int tick) throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeInt(tick);
        output.writeInt(0);
        return output.createPacket(PacketType.TICK);
    }

    public static Packet gameStart() throws IOException {
        return startGame();
    }

    public static Packet startGame() throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeByte(0);
        if (Rukkit.getRoundConfig().mapType == 0) {
            output.writeInt(0);
            output.writeString("maps/skirmish/" + Rukkit.getRoundConfig().mapName + ".tmx");
        } else if (Rukkit.getRoundConfig().mapType == 1) {
            output.writeInt(1);
            output.writeFile(CustomMapLoader.getStreamByName(Rukkit.getRoundConfig().mapName + ".tmx"));
            output.writeString(Rukkit.getRoundConfig().mapName + ".tmx");
        }
        output.writeBoolean(false);
        return output.createPacket(PacketType.START_GAME);
    }

    public static Packet serverInfo(RoundConfig config) throws IOException {
        return serverInfo(config, false, Rukkit.getModManager().fetchAllEnabledModUnits());
    }

    public static Packet serverInfo(RoundConfig config, Boolean isAdmin) throws IOException {
        return serverInfo(config, isAdmin, Rukkit.getModManager().fetchAllEnabledModUnits());
    }

    public static Packet serverInfo(RoundConfig config, boolean isAdmin, ArrayList<ModUnit> units)
            throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeString("com.corrodinggames.rts");
        output.writeInt(176);
        output.writeInt(config.mapType);
        output.writeString(config.mapName);
        output.writeInt(GameUtils.getMoneyFormat(config.credits));
        output.writeInt(config.fogType);
        output.writeBoolean(true);
        output.writeInt(1);
        output.writeByte(4);
        output.writeBoolean(false);
        output.writeBoolean(isAdmin);
        output.writeInt(Rukkit.getConfig().maxUnitsPerPlayer);
        output.writeInt(Rukkit.getConfig().maxUnitsPerPlayer);
        output.writeInt(config.startingUnits);
        output.writeFloat(config.income);
        output.writeBoolean(config.disableNuke);
        output.writeBoolean(false);
        output.writeBoolean(true);

        output.startBlock("customUnits", false);
        output.writeInt(1);
        output.writeInt(units.size());
        for (ModUnit unit : units) {
            output.writeString(unit.getUnitName());
            output.writeInt(unit.getUnitId());
            output.writeBoolean(true);
            if ("default".equals(unit.getModName())) {
                output.writeBoolean(false);
            } else {
                output.writeBoolean(true);
                output.writeString(unit.getModName());
            }
            output.writeLong(0);
            output.writeLong(0);
        }
        output.endBlock();

        output.writeBoolean(false);
        output.writeBoolean(false);
        output.writeBoolean(false);
        return output.createPacket(PacketType.SERVER_INFO);
    }

    public static Packet sandSave() throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeString("");
        return output.createPacket(PacketType.KICK);
    }

    public static Packet kick(String reason) throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeString(reason);
        return output.createPacket(PacketType.KICK);
    }

    public static Packet sendSave(int step, byte[] save, boolean isPullSave) throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeByte(0);
        output.writeInt(step);
        output.writeInt(step / 10);
        output.writeFloat(1.0f);
        output.writeFloat(1.0f);
        output.writeBoolean(isPullSave);
        output.writeBoolean(false);
        output.write(save);
        return output.createPacket(PacketType.SYNC);
    }

    public static Packet sendSave(NetworkRoom room, byte[] save, boolean isPullSave) throws IOException {
        return sendSave(room.getCurrentStep(), save, isPullSave);
    }

    public static Packet sendSave(ServerRoom room, byte[] save, boolean isPullSave) throws IOException {
        return sendSave(room.getCurrentStep(), save, isPullSave);
    }

    public static Packet sendPullSave(int step) throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeByte(0);
        output.writeInt(step);
        output.writeInt(step / 10);
        output.writeFloat(1.0f);
        output.writeFloat(1.0f);
        output.writeBoolean(true);
        output.writeBoolean(false);
        output.startBlock("gameSave", false);
        output.write(Rukkit.getDefaultSave().arr);
        output.endBlock();
        return output.createPacket(PacketType.SYNC);
    }

    public static Packet sendPullSave(int step, byte[] save) throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeByte(0);
        output.writeInt(step);
        output.writeInt(step / 10);
        output.writeFloat(1.0f);
        output.writeFloat(1.0f);
        output.writeBoolean(true);
        output.writeBoolean(false);
        output.startBlock("gameSave", false);
        output.write(save);
        output.endBlock();
        return output.createPacket(PacketType.SYNC);
    }

    public static Packet sendPullSave(NetworkRoom room) throws IOException {
        return sendPullSave(room.getCurrentStep());
    }

    public static Packet sendPullSave(ServerRoom room) throws IOException {
        return sendPullSave(room.getCurrentStep());
    }

    public static Packet syncCheckSum(int step) throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeInt(step);
        output.writeLong(0);
        output.writeInt(15);
        for (int i = 0; i < 15; i++) {
            output.writeLong(0);
        }
        return output.createPacket(PacketType.SYNC_CHECKSUM);
    }

    public static Packet syncCheckSum(int step, CheckSumList checkSumList) throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeInt(step);
        output.writeLong(0);
        output.writeInt(checkSumList.getCheckList().size());
        for (CheckSumList.ChecksumItem item : checkSumList.getCheckList()) {
            output.writeLong(item.prefix);
        }
        return output.createPacket(PacketType.SYNC_CHECKSUM);
    }

    public static Packet syncCheckSum(NetworkRoom room) throws IOException {
        return syncCheckSum(room.getCurrentStep());
    }

    public static Packet syncCheckSum(ServerRoom room) throws IOException {
        return syncCheckSum(room.getCurrentStep());
    }

    public static Packet gamePing(int step, int index, PingType type, float x, float y) throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeInt(step);
        output.writeInt(1);
        output.startBlock("c", false);
        output.writeByte(index);
        output.writeBoolean(false);
        output.writeBoolean(false);
        output.writeBoolean(false);
        output.writeInt(-1);
        output.writeInt(-1);
        output.writeBoolean(false);
        output.writeBoolean(false);
        output.writeInt(0);
        output.writeBoolean(true);
        output.writeByte(0);
        output.writeBoolean(true);
        output.writeFloat(x);
        output.writeFloat(y);
        output.writeLong(-1);
        output.writeString("c_6_" + type.toString());
        output.writeBoolean(false);
        output.stream.writeShort(0);
        output.writeBoolean(false);
        output.writeInt(0);
        output.writeBoolean(false);
        output.endBlock();
        return output.createPacket(PacketType.TICK);
    }

    public static Packet gamePing(NetworkRoom room, int index, PingType type, float x, float y)
            throws IOException {
        return gamePing(room.getCurrentStep(), index, type, x, y);
    }

    public static Packet gamePing(ServerRoom room, int index, PingType type, float x, float y)
            throws IOException {
        return gamePing(room.getCurrentStep(), index, type, x, y);
    }

    public static Packet gameSummon(int step, String unit, float x, float y, int team) throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeInt(step);
        output.writeInt(1);
        output.startBlock("c", false);
        output.writeByte(team);
        output.writeBoolean(true);
        output.writeEnum(GameActions.BUILD);
        int unitType = -2;
        for (int i = 0; i < InternalUnit.units.length; i++) {
            if (InternalUnit.units[i].equals(unit)) {
                unitType = i;
                break;
            }
        }
        output.writeInt(unitType);
        if (unitType == -2) {
            output.writeString(unit);
        }
        output.writeFloat(x);
        output.writeFloat(y);
        output.writeLong(-1L);
        output.writeByte(42);
        output.writeFloat(1.0f);
        output.writeFloat(1.0f);
        output.writeBoolean(false);
        output.writeBoolean(false);
        output.writeBoolean(false);
        output.writeBoolean(false);
        output.writeBoolean(false);
        output.writeBoolean(false);
        output.writeInt(-1);
        output.writeInt(-1);
        output.writeBoolean(false);
        output.writeBoolean(false);
        output.writeInt(0);
        output.writeBoolean(false);
        output.writeBoolean(false);
        output.writeLong(-1);
        output.writeString(unit);
        output.writeBoolean(false);
        output.stream.writeShort(0);
        output.writeBoolean(true);
        output.writeByte(0);
        output.writeFloat(0);
        output.writeFloat(0);
        output.writeInt(5);
        output.writeInt(0);
        output.writeBoolean(false);
        output.endBlock();
        return output.createPacket(PacketType.TICK);
    }

    public static Packet gameSummon(int step, String unit, float x, float y) throws IOException {
        return gameSummon(step, unit, x, y, -1);
    }

    public static Packet gameSummon(NetworkRoom room, String unit, float x, float y) throws IOException {
        return gameSummon(room.getCurrentStep(), unit, x, y, -1);
    }

    public static Packet gameSummon(ServerRoom room, String unit, float x, float y) throws IOException {
        return gameSummon(room.getCurrentStep(), unit, x, y, -1);
    }

    public static Packet gameSummon(NetworkRoom room, String unit, float x, float y, int team)
            throws IOException {
        return gameSummon(room.getCurrentStep(), unit, x, y, team);
    }

    public static Packet gameSummon(ServerRoom room, String unit, float x, float y, int team)
            throws IOException {
        return gameSummon(room.getCurrentStep(), unit, x, y, team);
    }

    public static Packet gameSurrounder(int step, int index) throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeInt(step);
        output.writeInt(1);
        output.startBlock("c", false);
        output.writeByte(index);
        output.writeBoolean(false);
        output.writeBoolean(false);
        output.writeBoolean(false);
        output.writeInt(-1);
        output.writeInt(-1);
        output.writeBoolean(false);
        output.writeBoolean(false);
        output.writeInt(0);
        output.writeBoolean(false);
        output.writeBoolean(false);
        output.writeLong(-1);
        output.writeString("-1");
        output.writeBoolean(false);
        output.stream.writeShort(0);
        output.writeBoolean(true);
        output.writeByte(0);
        output.writeFloat(0);
        output.writeFloat(0);
        output.writeInt(100);
        output.writeInt(0);
        output.writeBoolean(false);
        output.endBlock();
        return output.createPacket(PacketType.TICK);
    }

    public static Packet gameSurrounder(NetworkRoom room, int index) throws IOException {
        return gameSurrounder(room.getCurrentStep(), index);
    }

    public static Packet gameSurrounder(ServerRoom room, int index) throws IOException {
        return gameSurrounder(room.getCurrentStep(), index);
    }

    public static Packet packetQuestion(int questionId, String question) throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeByte(1);
        output.writeInt(questionId);
        output.writeString(question);
        return output.createPacket(PacketType.QUESTION);
    }

    public static Packet packetReturnToBattleroom() throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeByte(0);
        return output.createPacket(PacketType.RETURN_TO_BATTLEROOM);
    }
}
