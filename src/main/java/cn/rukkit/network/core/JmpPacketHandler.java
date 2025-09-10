package cn.rukkit.network.core;

import cn.rukkit.network.core.packet.Packet;
import cn.rukkit.network.core.packet.PacketType;
import cn.rukkit.network.core.packet.UniversalPacket;
import cn.rukkit.network.io.GameInputStream;
import io.netty.channel.ChannelHandlerContext;

public class JmpPacketHandler extends PacketHandler {
    ChannelHandlerContext ctx = null;
    Packet packet = null;

    @Override
    public void handle() throws Exception {
        if (packet.type == PacketType.PREREGISTER_CONNECTION) {
            // =============== 实际对封包处理 =================//
            GameInputStream inStream = new GameInputStream(this.packet);
            inStream.readString();
            String queryString;
            int packetVersion = inStream.readInt();
            int clientVersion = inStream.readInt();
            if (clientVersion < 151) {
                // 非1.15无法正常游戏
                ctx.writeAndFlush(
                        UniversalPacket.kick("Your game ver isn't 1.15(code151), Your ver code: " + clientVersion));
            }

            // betaGameVersion = getBetaVersion(clientVersion);

            if (packetVersion >= 1) {
                inStream.skip(4);
            }
            if (packetVersion >= 2) {
                queryString = inStream.readIsString();
            }
            if (packetVersion >= 3) {
                // Player Name
                inStream.readString();
            }

            // return
            ctx.write(UniversalPacket.preRegister());
            ctx.writeAndFlush(UniversalPacket.fromRelayJumpsToAnotherServerInternalPacket("micro.xin:5123"));
        }else{
            //意外的Packet
        }
    }

    @Override
    public void updateMsg(ChannelHandlerContext ctx, Object msg) {
        this.ctx = ctx;
        this.packet = (Packet) msg;
        return;
    }

    @Override
    public void onConnectionClose(ChannelHandlerContext ctx) {
        return;
    }

}
