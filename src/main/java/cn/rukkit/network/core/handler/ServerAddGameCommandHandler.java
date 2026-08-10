/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.core.handler;

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
import cn.rukkit.network.command.GameCommand;
import cn.rukkit.network.core.packet.Packet;
import cn.rukkit.network.core.packet.PacketType;
import cn.rukkit.network.io.GameInputStream;
import cn.rukkit.network.io.GameOutputStream;
import cn.rukkit.network.io.GzipDecoder;
import cn.rukkit.network.io.GzipEncoder;
import cn.rukkit.network.room.ServerRoomConnection;
import cn.rukkit.util.MathUtil;

import java.io.DataInputStream;
import java.util.List;

/**
 * Master-compatible game command decoder for the core packet stack.
 *
 * <p>The command is decoded and re-encoded in the same shape as the master
 * handler so action events are raised before the command is broadcast to the
 * room. The concrete connection type is the only runtime-specific part.</p>
 */
public class ServerAddGameCommandHandler extends ServerPacketHandler {
    @Override
    public int getType() {
        return PacketType.ADD_GAMECOMMAND;
    }

    @Override
    public List<ConnectionState> getAllowedStates() {
        return List.of(ConnectionState.IN_GAME);
    }

    @Override
    public void handle(ServerPacketContext context, Packet packet) throws Exception {
        ServerRoomConnection connection = context.connection();
        if (connection == null || connection.player == null) {
            getLogger().warn("Ignore PACKET_ADD_GAMECOMMAND because connection is not ready.");
            return;
        }

        GameInputStream input = new GameInputStream(packet);
        GameCommand command = new GameCommand();
        command.arr = input.getDecodeBytes();

        GameInputStream commandInput = new GameInputStream(command.arr);
        GameOutputStream output = new GameOutputStream();
        Event actionEvent = null;

        byte index = commandInput.readByte();
        output.writeByte(index);
        getLogger().debug("-- Command Recording --");
        getLogger().debug("teamIndex={}", index);

        if (commandInput.readBoolean()) {
            getLogger().debug("-- BasicGameAction --");
            output.writeBoolean(true);
            GameActions action = commandInput.readEnum(GameActions.class);
            output.writeEnum(action);
            getLogger().debug("Action={}", action);

            int unitIndex = commandInput.readInt();
            output.writeInt(unitIndex);
            getLogger().debug("BuildUnit:{}", unitIndex);
            String targetUnit = "";
            if (unitIndex == -2) {
                targetUnit = commandInput.readString();
                output.writeString(targetUnit);
                getLogger().debug("Custom={}", targetUnit);
            }
            if (unitIndex != -1 && unitIndex != -2) {
                targetUnit = InternalUnit.units[unitIndex];
            }

            float x = commandInput.readFloat();
            output.writeFloat(x);
            float y = commandInput.readFloat();
            output.writeFloat(y);
            long targetUnitId = commandInput.readLong();
            output.writeLong(targetUnitId);
            getLogger().debug("TargetUnitID={}", targetUnitId);

            byte byte1 = commandInput.readByte();
            float float1 = commandInput.readFloat();
            float float2 = commandInput.readFloat();
            boolean bool1 = commandInput.readBoolean();
            boolean bool2 = commandInput.readBoolean();
            boolean bool3 = commandInput.readBoolean();
            output.writeByte(byte1);
            output.writeFloat(float1);
            output.writeFloat(float2);
            output.writeBoolean(bool1);
            output.writeBoolean(bool2);
            output.writeBoolean(bool3);

            if (commandInput.readBoolean()) {
                output.writeBoolean(true);
                String actionId = commandInput.readString();
                output.writeString(actionId);
                getLogger().debug("SPECIALACTIONID={}", actionId);
            } else {
                output.writeBoolean(false);
            }
            switch (action) {
                case BUILD:
                    actionEvent = new BuildEvent(connection.player, x, y,
                            targetUnitId, targetUnit);
                    break;
                case MOVE:
                    actionEvent = new MoveEvent(connection.player, x, y, targetUnitId);
                    break;
                default:
                    break;
            }
            getLogger().debug("-- End BasicGameAction --");
        } else {
            output.writeBoolean(false);
        }

        boolean bool4 = commandInput.readBoolean();
        boolean isCancel = commandInput.readBoolean();
        output.writeBoolean(bool4);
        output.writeBoolean(isCancel);

        int int1 = commandInput.readInt();
        int int2 = commandInput.readInt();
        output.writeInt(int1);
        output.writeInt(int2);

        if (commandInput.readBoolean()) {
            output.writeBoolean(true);
            output.writeFloat(commandInput.readFloat());
            output.writeFloat(commandInput.readFloat());
        } else {
            output.writeBoolean(false);
        }

        boolean bool6 = commandInput.readBoolean();
        int unitCount = commandInput.readInt();
        output.writeBoolean(bool6);
        output.writeInt(unitCount);
        for (int i = 0; i < unitCount; i++) {
            output.writeLong(commandInput.readLong());
        }

        // The field is the pre-command player. The original server binds it
        // to the connection that submitted the command instead of trusting
        // the player index supplied by the client.
        boolean hasCommandPlayer = commandInput.readBoolean();
        if (hasCommandPlayer) {
            commandInput.readByte();
        }
        output.writeBoolean(true);
        output.writeByte(connection.player.playerIndex);

        float pingX = 0;
        float pingY = 0;
        if (commandInput.readBoolean()) {
            output.writeBoolean(true);
            pingX = commandInput.readFloat();
            pingY = commandInput.readFloat();
            output.writeFloat(pingX);
            output.writeFloat(pingY);
        } else {
            output.writeBoolean(false);
        }

        long unitId = commandInput.readLong();
        output.writeLong(unitId);

        String buildUnit = commandInput.readString();
        if (!buildUnit.equals("-1")) {
            if (buildUnit.startsWith("c_6_")) {
                actionEvent = new PingEvent(connection.player, pingX, pingY, buildUnit);
            } else {
                actionEvent = new TaskEvent(connection.player, buildUnit, unitId, isCancel);
            }
        }
        output.writeString(buildUnit);

        boolean bool7 = commandInput.readBoolean();
        output.writeBoolean(bool7);

        commandInput.readShort();
        output.writeShort(connection.currectRoom.playerManager.getSharedControlMask());

        // A client cannot submit a system action. Consume the payload so the
        // decoder remains aligned, then clear the flag like the original
        // NetworkEngine.processCommandPacket().
        if (commandInput.readBoolean()) {
            commandInput.readByte();
            commandInput.readFloat();
            commandInput.readFloat();
            commandInput.readInt();
        }
        output.writeBoolean(false);

        int movementUnitCount = commandInput.readInt();
        output.writeInt(movementUnitCount);
        for (int i = 0; i < movementUnitCount; i++) {
            output.writeLong(commandInput.readLong());
            output.writeFloat(commandInput.readFloat());
            output.writeFloat(commandInput.readFloat());
            output.writeFloat(commandInput.readFloat());
            output.writeFloat(commandInput.readFloat());
            output.writeInt(commandInput.readInt());
            output.writeEnum(commandInput.readEnum(UnitType.class));

            if (commandInput.readBoolean()) {
                output.writeBoolean(true);
                if (commandInput.readBoolean()) {
                    output.writeBoolean(true);
                    GzipEncoder pathOutput = output.getEncodeStream("p", true);
                    byte[] bytes = commandInput.getDecodeBytes();
                    GzipDecoder decoder = new GzipDecoder(bytes);
                    DataInputStream pathInput = decoder.stream;

                    int pathCount = pathInput.readInt();
                    pathOutput.stream.writeInt(pathCount);
                    if (pathCount > 0) {
                        short unitX = pathInput.readShort();
                        short unitY = pathInput.readShort();
                        pathOutput.stream.writeShort(unitX);
                        pathOutput.stream.writeShort(unitY);
                        for (int pathIndex = 1; pathIndex < pathCount; pathIndex++) {
                            int length = pathInput.readByte();
                            pathOutput.stream.writeByte(length);
                            if (length < 128) {
                                int deltaX = (length & 3) - 1;
                                int deltaY = ((length & 12) >> 2) - 1;
                                if (MathUtil.abs(deltaX) > 1 || MathUtil.abs(deltaY) > 1) {
                                    getLogger().warn("Bad unit path.");
                                }
                                unitX = (short) (unitX + deltaX);
                                unitY = (short) (unitY + deltaY);
                            } else {
                                unitX = pathInput.readShort();
                                unitY = pathInput.readShort();
                                pathOutput.stream.writeShort(unitX);
                                pathOutput.stream.writeShort(unitY);
                            }
                        }
                    }
                    output.flushEncodeData(pathOutput);
                } else {
                    output.writeBoolean(false);
                }
            } else {
                output.writeBoolean(false);
            }
        }

        boolean finalFlag = commandInput.readBoolean();
        output.writeBoolean(finalFlag);
        getLogger().debug("-- Command recording end --");

        Packet gamePacket = output.createPacket(PacketType.TICK);
        command.arr = gamePacket.bytes;
        if (actionEvent != null) {
            ListenerList listenerList = (ListenerList) actionEvent.getClass()
                    .getMethod("getListenerList").invoke(null);
            if (listenerList.callListeners(actionEvent)) {
                connection.player.markCommandActivity();
                connection.sendGameCommand(command);
            } else {
                getLogger().debug("Event {} cancelled!", actionEvent);
            }
        } else {
            connection.player.markCommandActivity();
            connection.sendGameCommand(command);
        }
    }
}
