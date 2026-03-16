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
import cn.rukkit.network.packet.Packet;
import cn.rukkit.util.LangUtil;

import java.util.List;

public class PreRegisterHandler extends PacketHandler {
    @Override
    public int getType() {
        return Packet.PACKET_PREREGISTER_CONNECTION;
    }

    @Override
    public List<ConnectionState> getAllowedStates() {
        return List.of(ConnectionState.CONNECTED);
    }

    @Override
    public void handle(PacketContext ctx, Packet packet) throws Exception {
        getLogger().debug("Received PACKET_PREREGISTER_CONNECTION");
        getLogger().debug("New connection established:{}", ctx.ctx().channel().remoteAddress());
        ctx.ctx().write(Packet.preRegister());
        ctx.ctx().writeAndFlush(Packet.chat("SERVER", LangUtil.getString("rukkit.playerRegister"), -1));
        ctx.transitionTo(ConnectionState.PRE_REGISTERED);
    }
}
