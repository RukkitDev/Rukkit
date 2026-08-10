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
import cn.rukkit.network.core.packet.UniversalPacket;
import cn.rukkit.util.LangUtil;

import java.util.List;

/** Master-compatible pre-registration handler for the core packet stack. */
public class ServerPreRegisterHandler extends ServerPacketHandler {
    @Override
    public int getType() {
        return PacketType.PREREGISTER_CONNECTION;
    }

    @Override
    public List<ConnectionState> getAllowedStates() {
        return List.of(ConnectionState.CONNECTED);
    }

    @Override
    public void handle(ServerPacketContext context, Packet packet) throws Exception {
        getLogger().debug("Received PACKET_PREREGISTER_CONNECTION");
        getLogger().debug("New connection established:{}", context.ctx().channel().remoteAddress());
        context.ctx().write(UniversalPacket.preRegister());
        context.ctx().writeAndFlush(
                UniversalPacket.chat("SERVER", LangUtil.getString("rukkit.playerRegister"), -1));
        context.transitionTo(ConnectionState.PRE_REGISTERED);
    }
}
