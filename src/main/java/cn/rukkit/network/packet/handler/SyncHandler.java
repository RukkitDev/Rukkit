/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.packet.handler;

import cn.rukkit.game.SaveData;
import cn.rukkit.network.ConnectionState;
import cn.rukkit.network.GameInputStream;
import cn.rukkit.network.packet.Packet;

import java.util.List;

public class SyncHandler extends PacketHandler {
    @Override
    public int getType() {
        return Packet.PACKET_SYNC;
    }

    @Override
    public List<ConnectionState> getAllowedStates() {
        return List.of(ConnectionState.IN_GAME);
    }

    @Override
    public void handle(PacketContext ctx, Packet packet) throws Exception {
        if (ctx.connection() == null) {
            return;
        }
        GameInputStream in = new GameInputStream(packet);
        in.readByte();
        int frame = in.readInt();
        int time = in.readInt() / 15;
        getLogger().trace("sync frame={} payload: {}, {}, {}, {}", frame, in.readFloat(), in.readFloat(), in.readBoolean(), in.readBoolean());
        byte[] save = new byte[in.stream.available()];
        in.stream.read(save);
        if (save.length > 20) {
            SaveData data = new SaveData();
            data.arr = save;
            data.time = time;
            ctx.connection().save = data;
        }
    }
}

