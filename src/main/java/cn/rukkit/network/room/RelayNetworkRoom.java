/*
 * Copyright 2025 Micro(MCLDY@outlook.com) and contributors.
 * 
 * 本衍生作品基于 AGPLv3 许可证
 * This derivative work is licensed under AGPLv3
 */
package cn.rukkit.network.room;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

import cn.rukkit.Rukkit;
import cn.rukkit.config.RoundConfig;
import cn.rukkit.game.NetworkPlayer;
import cn.rukkit.game.PlayerManager;
import cn.rukkit.network.command.NewGameCommand;
import cn.rukkit.network.core.packet.Packet;
import cn.rukkit.network.core.packet.PacketType;
import cn.rukkit.network.core.packet.UniversalPacket;
import cn.rukkit.network.io.GameOutputStream;
import cn.rukkit.util.Vote;

public class RelayNetworkRoom {
    public PlayerManager playerManager;
    public RoomConnectionManager connectionManager;
    /**
     * 命令列表。在采用更稳定的同步(useCommandQuere)时会启用，减少同步错误但是会提高操作延迟。
     */
    private LinkedList<NewGameCommand> commandQuere = new LinkedList<NewGameCommand>();

    public RoundConfig config;
    public int stepRate = 200;
    public int currentStep = 0;
    public int checkSumFrame = 0;
    public final AtomicInteger checkSumReceived = new AtomicInteger();
    public int syncCount = 0;

    
    public int roomId;
    public RelayRoomConnection adminConn;
    public RelayRoomConnection connections[] = new RelayRoomConnection[9];
    public int site = -1;

    public boolean isGaming = false;
    public Packet startGamePacket = null;

    public Vote vote;

    @Override
    public String toString() {
        return "not set";
    }

    public RelayNetworkRoom(int id, RelayRoomConnection adminConn) {
        // 指定房间id
        roomId = id;
        this.adminConn = adminConn;
    }

    public void sendPackageToHOST(Packet p) throws IOException {
        GameOutputStream o = new GameOutputStream();
        o.writeInt(site);
        o.writeInt(p.bytes.length + 8);
        o.writeInt(p.bytes.length);
        o.writeInt(p.type);
        o.write(p.bytes);
        adminConn.handler.ctx.writeAndFlush(o.createPacket(PacketType.PACKET_FORWARD_CLIENT_FROM));
    }
    public void addconn(RelayRoomConnection conn){
        connections[0]=conn;
    }

    // =========================================

    /**
     * Broadcast a packet.
     * 
     * @param packet the packet wants to broadcast in this room.
     */
    public void broadcast(Packet packet) {
        connectionManager.broadcast(packet);
    }

    public void discard() {
        playerManager.reset();
        connectionManager.disconnect();
        connectionManager.clearAllSaveData();
        playerManager = null;
        connectionManager = null;
    }

    public void changeMapWhileRunning(String mapName, int type) {
        Rukkit.getRoundConfig().mapName = mapName;
        Rukkit.getRoundConfig().mapType = type;
        try {
            connectionManager.broadcast(UniversalPacket.gameStart());
            // Set shared control.
            if (Rukkit.getRoundConfig().sharedControl) {
                for (NetworkPlayer p : playerManager.getPlayerArray()) {
                    try {
                        p.isNull();
                        // p.isSharingControl = true;
                    } catch (NullPointerException ignored) {
                        continue;
                    }
                }
            }
            // Reset tick time
            currentStep = 0;
            // Broadcast start packet.
            connectionManager.broadcast(UniversalPacket.serverInfo(config));
            for (ServerRoomConnection conn : connectionManager.getConnections()) {
                conn.updateTeamList(false);
            }
        } catch (IOException ignored) {
        }
    }

    public void addCommand(NewGameCommand cmd) {
        if (Rukkit.getConfig().useCommandQuere) {
            commandQuere.addLast(cmd);
        } else {
            try {
                broadcast(UniversalPacket.gameCommand(this.currentStep, cmd));
            } catch (IOException ignored) {
            }
        }
    }
}
