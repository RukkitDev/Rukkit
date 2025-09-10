/*
 * Copyright 2025 Micro(MCLDY@outlook.com) and contributors.
 * 
 * 本衍生作品基于 AGPLv3 许可证
 * This derivative work is licensed under AGPLv3
 */
package cn.rukkit.network.command;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.rukkit.event.Event;
import cn.rukkit.event.action.BuildEvent;
import cn.rukkit.event.action.ExampleMoveEvent;
import cn.rukkit.event.action.ExampleMoveEvent.moveingUnitInfo;
import cn.rukkit.event.action.MoveEvent;
import cn.rukkit.event.action.PingEvent;
import cn.rukkit.event.action.TaskEvent;
import cn.rukkit.game.GameActions;
import cn.rukkit.game.NetworkPlayer;
import cn.rukkit.game.UnitType;
import cn.rukkit.game.unit.InternalUnit;
import cn.rukkit.network.core.ServerPacketHandler;
import cn.rukkit.network.io.GameInputStream;
import cn.rukkit.network.io.GameOutputStream;
import cn.rukkit.network.io.GzipDecoder;
import cn.rukkit.network.io.GzipEncoder;
import cn.rukkit.network.room.ServerRoomConnection;
import cn.rukkit.util.MathUtil;

//用来耦合游戏底层tick的
//代码十分抽象 建议别动
public class NewGameCommand {
    private static final Logger log = LoggerFactory.getLogger(ServerPacketHandler.class);
    public GameOutputStream out = new GameOutputStream();// 用来烂代码的
    // 关键数据
    public byte[] arr; // 实际包体
    public List<Event> acts = new ArrayList<Event>();// 执行Action
    public byte index;// 玩家偏移

    /*
     * 关于耦合区:
     * 每次游戏更新时都需要更新GameCommand的内容
     * 为了方便更新 划分出耦合区
     * 开发者需要在耦合区获取必要数据 例如Action
     * 非耦合区完成业务代码 例如Event
     * 这样可以专心耦合代码 不必每次都在耦合代码中写业务代码
     */


    public NewGameCommand(byte[] arr, ServerRoomConnection conn) throws IOException {
        this.arr = arr;

        // 在这里写需要耦合区获取的变量
        boolean isBasicAction;
        boolean isCancel;// BasicAction | 是否为取消操作
        float x = -1;// BasicAction
        float y = -1;// BasicAction
        String targetUnit = "";// BasicAction
        GameActions action = null;// BaseAction
        boolean isPing;
        float pingX = 0;
        float pingY = 0;
        boolean isBuildUnit = false;// 建造单位
        String buildUnit;
        List<Long> actionUnitsIds = new ArrayList<Long>();// 受到执行的所有单位IDs
        int movingUnitTypeCount;//
        List<moveingUnitInfo> moveingUnits = new ArrayList<moveingUnitInfo>();
        // =================================================================================//
        // ------------------------------------耦合区开始------------------------------------//
        // ==================================================================================//
        GameInputStream str = new GameInputStream(arr);
        // boolean isCustom = false;
        byte byte1, byte2;
        // long l1, l2, l3, l4;
        index = str.readByte();
        out.writeByte(index);
        log.debug("-- Command Recording --");
        log.debug("teamIndex=" + index);

        // 是否为BasicAction
        isBasicAction = str.readBoolean();
        if (isBasicAction) {
            log.debug("-- BasicGameAction --");
            out.writeBoolean(true);
            // 游戏指令
            action = (GameActions) str.readEnum(GameActions.class);
            out.writeEnum(action);
            log.debug("Action=" + action);
            // sendPacket(ctx, new Packet().chat("Debug", "Your action is:" + action, -1));
            // 对应单位id，如果为-1则无建造
            int n2 = str.readInt();
            out.writeInt(n2);
            log.debug("BuildUnit:" + n2);
            if (n2 == -2) {
                targetUnit = str.readString();
                out.writeString(targetUnit);
                // ((BuildAction)act).targetUnit = unit;
                log.debug("Custom=" + targetUnit);
                // sendPacket(ctx, new Packet().chat("Debug", "(External)You are building by
                // builder:" + targetUnit, -1));
            }

            if (n2 != -1 && n2 != -2) {
                targetUnit = InternalUnit.units[n2];
                // sendPacket(ctx, new Packet().chat("Debug", "You are building by builder:" +
                // InternalUnit.units[n2], -1));
            }

            // 动作的目标位置
            x = str.readFloat();
            out.writeFloat(x);
            // act.x = x;
            log.debug("" + x);
            y = str.readFloat();
            out.writeFloat(y);
            // act.y = y;
            log.debug("" + y);
            // sendPacket(ctx, new Packet().chat("Debug", "Quered: x=" + x + " y=" + y,
            // -1));

            // 对应单位的id(单位把执行动作到..上)
            long targetUnitID = str.readLong();
            out.writeLong(targetUnitID);
            log.debug("TargetUnitID=" + targetUnitID);

            byte1 = str.readByte();
            float float1 = str.readFloat();
            float float2 = str.readFloat();
            boolean bool1 = str.readBoolean();
            boolean bool2 = str.readBoolean();
            boolean bool3 = str.readBoolean();
            log.trace("Byte1=" + byte1);
            log.trace("Float1=" + float1);
            log.trace("Float2=" + float2);
            log.trace("Boolean1=" + bool1);
            log.trace("Boolean2=" + bool2);
            log.debug("Boolean3=" + bool3);
            out.writeByte(byte1);
            out.writeFloat(float1);
            out.writeFloat(float2);
            out.writeBoolean(bool1);
            out.writeBoolean(bool2);
            out.writeBoolean(bool3);
            // 1.14新增
            if (str.readBoolean()) {
                out.writeBoolean(true);
                String actionId = str.readString();
                log.debug("SPECIALACTIONID=" + actionId);
                out.writeString(actionId);
            } else {
                out.writeBoolean(false);
            }
            log.debug("-- End BasicGameAction --");
        } else {
            out.writeBoolean(false);
        }
        //
        log.trace("CommandBlock ended.");

        boolean bool4;
        bool4 = str.readBoolean(); // 未知
        isCancel = str.readBoolean();
        log.trace("Boolean4=" + bool4);
        // 是否为取消操作
        log.debug("isCancel=" + isCancel);
        out.writeBoolean(bool4);
        out.writeBoolean(isCancel);

        int int1, int2;
        int1 = str.readInt(); // SpecialAction 对应的id
        int2 = str.readInt(); // 单位的攻击模式
        log.debug("SpecialActionId=" + int1);
        log.debug("AttackMode=" + int2);
        out.writeInt(int1);
        out.writeInt(int2);

        // 疑似和核弹等目标发射有关，设置集结点
        if (str.readBoolean()) {
            out.writeBoolean(true);
            log.trace("A readBoolean is true.");
            float f3, f4;
            f3 = str.readFloat();
            f4 = str.readFloat();
            log.trace("Float3=" + f3);
            log.trace("Float4=" + f4);
            out.writeFloat(f3);
            out.writeFloat(f4);
        } else {
            out.writeBoolean(false);
        }

        // 未知
        boolean bool6 = str.readBoolean();
        log.trace("Boolean6=" + bool6);

        // 批量执行单位
        {
            // 这个是按Unit计算的 每个受到操作的Unit都会被计入
            int t = str.readInt();
            out.writeBoolean(bool6);
            out.writeInt(t);
            // 批量执行的单位数量
            log.debug("UnitCount(Really)=" + t);
            // 批量执行的单位ID
            for (int i = 0; i < t; i++) {
                long unitidInMatch = str.readLong();
                actionUnitsIds.add(unitidInMatch);
                log.debug("UnitidInMatch(Maybe)=" + unitidInMatch);
                out.writeLong(unitidInMatch);
            }
        }

        // 如果为True, 则为预执行命令
        if (str.readBoolean()) {
            out.writeBoolean(true);
            log.trace("A readBoolean is true.");
            byte2 = str.readByte(); // 玩家
            log.trace("Byte2=" + byte2);
            out.writeByte(byte2);
        } else {
            out.writeBoolean(false);
        }

        // 与Ping有关
        isPing = str.readBoolean();
        if (isPing) {
            out.writeBoolean(true);
            log.trace("Its a ping packet.");
            // float f5,f6;
            pingX = str.readFloat();
            pingY = str.readFloat();
            log.debug("PingX={} , PingY={}", pingX, pingY);
            out.writeFloat(pingX);
            out.writeFloat(pingY);
        } else {
            out.writeBoolean(false);
        }

        // 执行的单位id
        long l6 = str.readLong();
        log.debug("CheckUnitID=" + l6);
        out.writeLong(l6);

        // Build块（实际上叫做SpecialAction, 可能是ping，或者是build)
        buildUnit = str.readString();
        log.debug("str(BuildUnit):" + buildUnit);
        if (!buildUnit.equals("-1")) {
            isBuildUnit = true;
            /*
             * act = new ProduceAction();
             * ((ProduceAction) act).targetUnit = buildUnit;
             */
            // sendPacket(ctx, new Packet().chat("Debug", "You are building: " + buildUnit,
            // -1));
        }
        out.writeString(buildUnit);

        // 为true时，单位完成除建造和修复外的所有路径行动
        boolean bool7 = str.readBoolean();
        log.trace("Boolean7=" + bool7);
        out.writeBoolean(bool7);

        // 能否进行玩家控制的关键代码,填写32767即可。
        short short1 = str.readShort();
        log.trace("Short1=" + short1);
        out.stream.writeShort(32767);

        // SystemAction 比较危险，可以默认舍弃
        if (str.readBoolean()) {
            log.trace("A readBoolean is true.");
            out.writeBoolean(true);
            str.readByte();
            out.writeByte(0);
            float f1, f2;
            int i1;
            f1 = str.readFloat();
            f2 = str.readFloat();
            i1 = str.readInt();
            log.trace("{}, {}, {}", f1, f2, i1);
            out.writeFloat(f1);
            out.writeFloat(f2);
            out.writeInt(i1);
        } else {
            out.writeBoolean(false);
        }

        // 这个与上面的遍历操作单位不一样 是按种类执行的 会取种类其中的一个样例给出start/end pos
        // 即使9999机枪+9999建造者movingUnitTypeCount为2
        movingUnitTypeCount = str.readInt();
        out.writeInt(movingUnitTypeCount);
        log.debug("type count={}", movingUnitTypeCount);
        for (int i = 0; i < movingUnitTypeCount; i++) {
            float sx, sy, ex, ey;
            long unitid = str.readLong();
            sx = str.readFloat();
            sy = str.readFloat();
            ex = str.readFloat();
            ey = str.readFloat();
            out.writeLong(unitid);
            out.writeFloat(sx);
            out.writeFloat(sy);
            out.writeFloat(ex);
            out.writeFloat(ey);
            // 当前时间刻
            int timestamp = str.readInt();
            out.writeInt(timestamp);
            // 单位类型（1陆，潜艇，跨悬崖，跨悬崖跨水，悬浮5)
            UnitType u = (UnitType) str.readEnum(UnitType.class);
            log.debug("id={} startPos=({}, {}), endPos=({}, {}), startStamp={}, unitType={}", unitid, sx, sy,
                    ex, ey, timestamp, u);

            //记录样例 ExampleMoveEvent专属
            moveingUnits.add(new moveingUnitInfo(sx,sy,ex,ey,unitid));
            out.writeEnum(u);

            // Path 有关内容
            if (str.readBoolean()) { // 是否存在path
                out.writeBoolean(true);
                // 正式开始读取
                if (str.readBoolean()) { // 是否存在path
                    out.writeBoolean(true);
                    GzipEncoder outstr = out.getEncodeStream("p", true);
                    byte[] bytes = str.getDecodeBytes();
                    GzipDecoder dec = new GzipDecoder(bytes);
                    DataInputStream ins = dec.stream;

                    // path大小
                    int pathCount = ins.readInt();
                    outstr.stream.writeInt(pathCount);
                    if (pathCount > 0) {
                        short unitx = ins.readShort();
                        short unity = ins.readShort();
                        outstr.stream.writeShort(unitx);
                        outstr.stream.writeShort(unity);
                        log.debug("Start x:" + unitx + ", Start y:" + unity);
                        for (int i2 = 1; i2 < pathCount; i2++) {
                            // PathSize
                            int len = ins.readByte();
                            outstr.stream.writeByte(len);
                            if (len < 128) {
                                int i6 = (len & 3) - 1;
                                int i7 = ((len & 12) >> 2) - 1;
                                boolean bool = MathUtil.abs(i6) > 1 || MathUtil.abs(i7) > 1;
                                if (bool) {
                                    log.warn("Bad unit path.");
                                }
                                unitx = (short) (unitx + i6);
                                unity = (short) (unity + i7);
                            } else {
                                unitx = ins.readShort();
                                unity = ins.readShort();
                                outstr.stream.writeShort(unitx);
                                outstr.stream.writeShort(unity);
                            }
                            // log.debug(ins.readShort());
                        }
                    }
                    out.flushEncodeData(outstr);
                } else {
                    out.writeBoolean(false);
                }
            } else {
                out.writeBoolean(false);
            }
        }

        boolean bool = str.readBoolean();
        out.writeBoolean(bool);
        log.debug("-- Command recording end --");

        // byte[] byt = new byte[str.stream.available()];
        // str.stream.read(byt);
        // out.stream.write(byt);

        // ==================================================================================
        // //
        // ------------------------------------耦合区结束------------------------------------
        // //
        // ==================================================================================
        // //

        if (isBasicAction) {
            if (action == null || x == -1 || y == -1) {
                log.warn("WTF? BasicAction没获取到数据?");
            }
            switch (action) {
                case BUILD:
                    acts.add(new BuildEvent(conn.player, x, y, actionUnitsIds, targetUnit));
                    break;// 建造只会有一个建筑建造 对吧? 当然不对
                case MOVE:
                    acts.add(new MoveEvent(conn.player, x, y, actionUnitsIds));//
                    break;
                default:
                    // log.warn("wtf? no build&move base Action?");
                    break;
            }
        }
        if (isBuildUnit) {
            if (buildUnit.startsWith("c_6_")) {
                acts.add(new PingEvent(conn.player, pingX, pingY, buildUnit));
            } else {
                acts.add(new TaskEvent(conn.player, buildUnit, l6, isCancel));
            }
        }
        if(movingUnitTypeCount>0){
            for (moveingUnitInfo moveingUnitInfo : moveingUnits) {
                acts.add(new ExampleMoveEvent(conn.player, moveingUnitInfo));
            }
        }
    }
}
