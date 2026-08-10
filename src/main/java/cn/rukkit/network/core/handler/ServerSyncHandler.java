/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.core.handler;

import cn.rukkit.game.SaveData;
import cn.rukkit.network.ConnectionState;
import cn.rukkit.network.core.packet.Packet;
import cn.rukkit.network.core.packet.PacketType;
import cn.rukkit.network.io.GameInputStream;

import java.util.List;

/** Master-compatible save synchronization handler for the core packet stack. */
public class ServerSyncHandler extends ServerPacketHandler {
    @Override
    public int getType() {
        return PacketType.SYNC;
    }

    @Override
    public List<ConnectionState> getAllowedStates() {
        return List.of(ConnectionState.IN_GAME);
    }

    @Override
    public void handle(ServerPacketContext context, Packet packet) throws Exception {
        if (context.connection() == null) {
            return;
        }

        GameInputStream input = new GameInputStream(packet);
        input.readByte();
        int frame = input.readInt();
        int time = input.readInt() / 15;
        getLogger().trace("sync frame={} payload: {}, {}, {}, {}",
                frame,
                input.readFloat(),
                input.readFloat(),
                input.readBoolean(),
                input.readBoolean());

        byte[] save = input.getBlockRaw("gameSave");
        if (save.length > 20) {
            SaveData data = new SaveData();
            data.arr = save;
            data.time = time;
            context.connection().save = data;
        }
    }
}
