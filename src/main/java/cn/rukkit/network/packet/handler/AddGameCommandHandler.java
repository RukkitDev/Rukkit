/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.packet.handler;

import cn.rukkit.event.Event;
import cn.rukkit.event.ListenerList;
import cn.rukkit.event.action.BuildEvent;
import cn.rukkit.event.action.MoveEvent;
import cn.rukkit.event.action.PingEvent;
import cn.rukkit.event.action.TaskEvent;
import cn.rukkit.game.GameActions;
import cn.rukkit.game.UnitType;
import cn.rukkit.game.unit.InternalUnit;
import cn.rukkit.network.ConnectionState;
import cn.rukkit.network.GameInputStream;
import cn.rukkit.network.GameOutputStream;
import cn.rukkit.network.GzipDecoder;
import cn.rukkit.network.GzipEncoder;
import cn.rukkit.network.RoomConnection;
import cn.rukkit.network.command.GameCommand;
import cn.rukkit.network.packet.Packet;
import cn.rukkit.util.MathUtil;

import java.io.DataInputStream;
import java.util.List;

public class AddGameCommandHandler extends PacketHandler {

    @Override
    public int getType() {
        return Packet.PACKET_ADD_GAMECOMMAND;
    }

    @Override
    public List<ConnectionState> getAllowedStates() {
        return List.of(ConnectionState.IN_GAME);
    }

    @Override
    public void handle(PacketContext ctx, Packet packet) throws Exception {
        RoomConnection connection = ctx.connection();
        if (connection == null || connection.player == null) {
            getLogger().warn("Ignore PACKET_ADD_GAMECOMMAND because connection is not ready.");
            return;
        }
        GameInputStream in = new GameInputStream(packet);
        GameCommand cmd = new GameCommand();
        cmd.arr = in.getDecodeBytes();

        GameInputStream str = new GameInputStream(cmd.arr);
        GameOutputStream out = new GameOutputStream();
        Event act = null;

        byte index = str.readByte();
        out.writeByte(index);
        getLogger().debug("-- Command Recording --");
        getLogger().debug("teamIndex={}", index);

        if (str.readBoolean()) {
            getLogger().debug("-- BasicGameAction --");
            out.writeBoolean(true);
            GameActions action = (GameActions) str.readEnum(GameActions.class);
            out.writeEnum(action);
            getLogger().debug("Action={}", action);

            int n2 = str.readInt();
            out.writeInt(n2);
            getLogger().debug("BuildUnit:{}", n2);
            String targetUnit = "";
            if (n2 == -2) {
                targetUnit = str.readString();
                out.writeString(targetUnit);
                getLogger().debug("Custom={}", targetUnit);
            }
            if (n2 != -1 && n2 != -2) {
                targetUnit = InternalUnit.units[n2];
            }

            float x = str.readFloat();
            out.writeFloat(x);
            float y = str.readFloat();
            out.writeFloat(y);
            long targetUnitID = str.readLong();
            out.writeLong(targetUnitID);
            getLogger().debug("TargetUnitID={}", targetUnitID);

            byte byte1 = str.readByte();
            float float1 = str.readFloat();
            float float2 = str.readFloat();
            boolean bool1 = str.readBoolean();
            boolean bool2 = str.readBoolean();
            boolean bool3 = str.readBoolean();
            out.writeByte(byte1);
            out.writeFloat(float1);
            out.writeFloat(float2);
            out.writeBoolean(bool1);
            out.writeBoolean(bool2);
            out.writeBoolean(bool3);

            if (str.readBoolean()) {
                out.writeBoolean(true);
                String actionId = str.readString();
                out.writeString(actionId);
                getLogger().debug("SPECIALACTIONID={}", actionId);
            } else {
                out.writeBoolean(false);
            }
            switch (action) {
                case BUILD:
                    act = new BuildEvent(connection.player, x, y, targetUnitID, targetUnit);
                    break;
                case MOVE:
                    act = new MoveEvent(connection.player, x, y, targetUnitID);
                    break;
            }
            getLogger().debug("-- End BasicGameAction --");
        } else {
            out.writeBoolean(false);
        }

        boolean bool4 = str.readBoolean();
        boolean isCancel = str.readBoolean();
        out.writeBoolean(bool4);
        out.writeBoolean(isCancel);

        int int1 = str.readInt();
        int int2 = str.readInt();
        out.writeInt(int1);
        out.writeInt(int2);

        if (str.readBoolean()) {
            out.writeBoolean(true);
            float f3 = str.readFloat();
            float f4 = str.readFloat();
            out.writeFloat(f3);
            out.writeFloat(f4);
        } else {
            out.writeBoolean(false);
        }

        boolean bool6 = str.readBoolean();
        int t = str.readInt();
        out.writeBoolean(bool6);
        out.writeInt(t);
        for (int i = 0; i < t; i++) {
            out.writeLong(str.readLong());
        }

        // The field is the pre-command player. The original server binds it
        // to the connection that submitted the command instead of trusting
        // the player index supplied by the client.
        boolean hasCommandPlayer = str.readBoolean();
        if (hasCommandPlayer) {
            str.readByte();
        }
        out.writeBoolean(true);
        out.writeByte(connection.player.playerIndex);

        float pingX = 0;
        float pingY = 0;
        if (str.readBoolean()) {
            out.writeBoolean(true);
            pingX = str.readFloat();
            pingY = str.readFloat();
            out.writeFloat(pingX);
            out.writeFloat(pingY);
        } else {
            out.writeBoolean(false);
        }

        long l6 = str.readLong();
        out.writeLong(l6);

        String buildUnit = str.readString();
        if (!buildUnit.equals("-1")) {
            if (buildUnit.startsWith("c_6_")) {
                act = new PingEvent(connection.player, pingX, pingY, buildUnit);
            } else {
                act = new TaskEvent(connection.player, buildUnit, l6, isCancel);
            }
        }
        out.writeString(buildUnit);

        boolean bool7 = str.readBoolean();
        out.writeBoolean(bool7);

        str.readShort();
        out.writeShort(connection.currectRoom.playerManager.getSharedControlMask());

        // The original server never forwards a system action supplied by a
        // client. Consume its payload to keep the stream aligned, then clear
        // the flag before broadcasting the normal command.
        if (str.readBoolean()) {
            str.readByte();
            str.readFloat();
            str.readFloat();
            str.readInt();
        }
        out.writeBoolean(false);

        int movementUnitCount = str.readInt();
        out.writeInt(movementUnitCount);
        for (int i = 0; i < movementUnitCount; i++) {
            long unitid = str.readLong();
            float sx = str.readFloat();
            float sy = str.readFloat();
            float ex = str.readFloat();
            float ey = str.readFloat();
            out.writeLong(unitid);
            out.writeFloat(sx);
            out.writeFloat(sy);
            out.writeFloat(ex);
            out.writeFloat(ey);
            int timestamp = str.readInt();
            out.writeInt(timestamp);
            UnitType unitType = (UnitType) str.readEnum(UnitType.class);
            out.writeEnum(unitType);

            if (str.readBoolean()) {
                out.writeBoolean(true);
                if (str.readBoolean()) {
                    out.writeBoolean(true);
                    GzipEncoder outstr = out.getEncodeStream("p", true);
                    byte[] bytes = str.getDecodeBytes();
                    GzipDecoder dec = new GzipDecoder(bytes);
                    DataInputStream ins = dec.stream;

                    int pathCount = ins.readInt();
                    outstr.stream.writeInt(pathCount);
                    if (pathCount > 0) {
                        short unitx = ins.readShort();
                        short unity = ins.readShort();
                        outstr.stream.writeShort(unitx);
                        outstr.stream.writeShort(unity);
                        for (int i2 = 1; i2 < pathCount; i2++) {
                            int len = ins.readByte();
                            outstr.stream.writeByte(len);
                            if (len < 128) {
                                int i6 = (len & 3) - 1;
                                int i7 = ((len & 12) >> 2) - 1;
                                boolean invalidPath = MathUtil.abs(i6) > 1 || MathUtil.abs(i7) > 1;
                                if (invalidPath) {
                                    getLogger().warn("Bad unit path.");
                                }
                                unitx = (short) (unitx + i6);
                                unity = (short) (unity + i7);
                            } else {
                                unitx = ins.readShort();
                                unity = ins.readShort();
                                outstr.stream.writeShort(unitx);
                                outstr.stream.writeShort(unity);
                            }
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
        getLogger().debug("-- Command recording end --");

        Packet gamePacket = out.createPacket(Packet.PACKET_TICK);
        cmd.arr = gamePacket.bytes;
        if (act != null) {
            ListenerList list = (ListenerList) act.getClass().getMethod("getListenerList").invoke(null);
            if (list.callListeners(act)) {
                connection.player.markCommandActivity();
                connection.sendGameCommand(cmd);
            } else {
                getLogger().debug("Event {} cancelled!", act);
            }
        } else {
            connection.player.markCommandActivity();
            connection.sendGameCommand(cmd);
        }
    }
}
