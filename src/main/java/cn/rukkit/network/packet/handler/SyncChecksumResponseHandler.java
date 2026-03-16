/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.packet.handler;

import cn.rukkit.network.ConnectionState;
import cn.rukkit.network.GameInputStream;
import cn.rukkit.network.packet.Packet;

import java.io.DataInputStream;
import java.util.List;

public class SyncChecksumResponseHandler extends PacketHandler {
    @Override
    public int getType() {
        return Packet.PACKET_SYNC_CHECKSUM_RESPONCE;
    }

    @Override
    public List<ConnectionState> getAllowedStates() {
        return List.of(ConnectionState.IN_GAME);
    }

    @Override
    public void handle(PacketContext ctx, Packet packet) throws Exception {
        if (ctx.connection() == null || ctx.connection().player == null) {
            return;
        }
        GameInputStream in = new GameInputStream(packet);
        in.readByte();
        int serverTick = in.readInt();
        int clientTick = in.readInt();
        getLogger().info("[{}] Server tick: {}, Client tick: {}", ctx.connection().player.name, serverTick, clientTick);
        ctx.connection().lastSyncTick = clientTick;
        if (in.readBoolean()) {
            getLogger().info("Player {} send checksum!", ctx.connection().player.name);
            in.readLong();
            in.readLong();
            DataInputStream din = in.getUnDecodeStream();
            din.readInt();
            int checkSumCount = din.readInt();
            getLogger().debug("Total checksum: {}", checkSumCount);
            for (int i = 0; i < checkSumCount; i++) {
                din.readLong();
                long clientCheckData = din.readLong();
                getLogger().trace("{}: client={}", ctx.connection().player.checkList.get(i).getDescription(), clientCheckData);
                ctx.connection().player.checkList.get(i).setCheckData(clientCheckData);
            }
            ctx.connection().currectRoom.checkSumReceived.incrementAndGet();
            ctx.connection().checkSumSent = true;
            synchronized (ctx.connection().currectRoom.checkSumReceived) {
                ctx.connection().currectRoom.checkSumReceived.notifyAll();
            }
        } else {
            getLogger().info("Player {} did'n send checksum!We can sent back again!", ctx.connection().player.name);
            ctx.connection().doChecksum();
        }
    }
}

