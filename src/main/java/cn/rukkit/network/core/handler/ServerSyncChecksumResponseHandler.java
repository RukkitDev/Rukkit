/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.core.handler;

import cn.rukkit.network.ConnectionState;
import cn.rukkit.network.core.packet.Packet;
import cn.rukkit.network.core.packet.PacketType;
import cn.rukkit.network.io.GameInputStream;

import java.io.DataInputStream;
import java.util.List;

/** Master-compatible checksum response handler for the core packet stack. */
public class ServerSyncChecksumResponseHandler extends ServerPacketHandler {
    @Override
    public int getType() {
        return PacketType.SYNC_CHECKSUM_RESPONSE;
    }

    @Override
    public List<ConnectionState> getAllowedStates() {
        return List.of(ConnectionState.IN_GAME);
    }

    @Override
    public void handle(ServerPacketContext context, Packet packet) throws Exception {
        if (context.connection() == null || context.connection().player == null) {
            return;
        }

        GameInputStream input = new GameInputStream(packet);
        input.readByte();
        int serverTick = input.readInt();
        int clientTick = input.readInt();
        getLogger().info("[{}] Server tick: {}, Client tick: {}",
                context.connection().player.name, serverTick, clientTick);
        context.connection().lastSyncTick = clientTick;

        if (input.readBoolean()) {
            getLogger().info("Player {} send checksum!", context.connection().player.name);
            input.readLong();
            input.readLong();
            DataInputStream checksumStream = input.getUnDecodeStream();
            checksumStream.readInt();
            int checksumCount = checksumStream.readInt();
            getLogger().debug("Total checksum: {}", checksumCount);
            for (int i = 0; i < checksumCount; i++) {
                checksumStream.readLong();
                long clientCheckData = checksumStream.readLong();
                getLogger().trace("{}: client={}",
                        context.connection().player.checkList.get(i).getDescription(),
                        clientCheckData);
                context.connection().player.checkList.get(i).setCheckData(clientCheckData);
            }

            context.connection().currectRoom.checkSumReceived.incrementAndGet();
            context.connection().checkSumSent = true;
            synchronized (context.connection().currectRoom.checkSumReceived) {
                context.connection().currectRoom.checkSumReceived.notifyAll();
            }
        } else {
            getLogger().info("Player {} did'n send checksum!We can sent back again!",
                    context.connection().player.name);
            context.connection().doChecksum();
        }
    }
}
