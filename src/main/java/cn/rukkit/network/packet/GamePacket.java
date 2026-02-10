/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.packet;

import cn.rukkit.game.PingType;
import cn.rukkit.network.NetworkRoom;

/**
 * Packet 抽象层，负责游戏内基础 Packet 的抽象
 * @since 0.9.2
 */
public abstract class GamePacket {
    public abstract Packet kick(String reason);
    public abstract Packet sendSave(int step, byte[] bArr, boolean isPullSave);
    public abstract Packet sendPullSave(int step);
    public abstract Packet syncCheckSum(int step);
    public abstract Packet gamePing(int step, int index, PingType type, float x, float y);
    public abstract Packet gameSummon(int step, String unit, float x, float y, int team);
    public abstract Packet gameSurrender(int step, int index);
    public abstract Packet packetQuestion(int qid, String question);
    public abstract Packet packetReturnToBattleroom();
}
