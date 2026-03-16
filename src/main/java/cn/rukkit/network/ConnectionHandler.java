/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network;

import cn.rukkit.Rukkit;
import cn.rukkit.event.player.PlayerLeftEvent;
import cn.rukkit.network.packet.Packet;
import cn.rukkit.network.packet.handler.PacketContext;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledFuture;

public class ConnectionHandler extends ChannelInboundHandlerAdapter {
    private final Logger log = LoggerFactory.getLogger(ConnectionHandler.class);

    public ChannelHandlerContext ctx;
    private RoomConnection conn;
    private ConnectionState state = ConnectionState.CONNECTED;
    private ScheduledFuture<?> timeoutFuture;

    private NetworkRoom currentRoom;
    private String disconnectReason = "Unknown";

    public class TimeoutTask implements Runnable {
        private int execTime = 0;

        @Override
        public void run() {
            execTime++;
            if (execTime >= Rukkit.getConfig().registerTimeout) {
                ctx.disconnect();
            }
        }
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.ctx = ctx;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        startTimeout();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        setState(ConnectionState.DISCONNECTED);
        if (conn != null) {
            PlayerLeftEvent.getListenerList().callListeners(new PlayerLeftEvent(conn.player, disconnectReason));
            if (currentRoom != null) {
                currentRoom.connectionManager.discard(conn);
            }
            Rukkit.getGlobalConnectionManager().discard(conn);
            conn.stopPingTask();
            conn.stopTeamTask();
        } else {
            String remote = String.valueOf(ctx.channel().remoteAddress());
            if (!(remote.contains("18.216.139.119") || remote.contains("192.241.156.189"))) {
                log.warn("There is a unexpected connection at connection {}.", remote);
            }
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            Packet packet = (Packet) msg;
            if (!Rukkit.getPacketHandlerManager().dispatch(new PacketContext(ctx, conn, this), packet)) {
                log.debug("Unhandled packet type {} from {}", packet.type, ctx.channel().remoteAddress());
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.warn("Exception happened", cause);
    }

    public ConnectionState getState() {
        return state;
    }

    public void setState(ConnectionState state) {
        this.state = state;
    }

    public RoomConnection getConn() {
        return conn;
    }

    public void setConn(RoomConnection conn) {
        this.conn = conn;
        if (conn != null) {
            this.currentRoom = conn.currectRoom;
        }
    }

    public NetworkRoom getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(NetworkRoom currentRoom) {
        this.currentRoom = currentRoom;
    }

    public String getDisconnectReason() {
        return disconnectReason;
    }

    public void setDisconnectReason(String disconnectReason) {
        this.disconnectReason = disconnectReason;
    }

    public void startTimeout() {
        if (timeoutFuture == null) {
            timeoutFuture = Rukkit.getThreadManager().schedule(new TimeoutTask(), 1000, 1000);
        }
    }

    public void stopTimeout() {
        if (timeoutFuture != null) {
            Rukkit.getThreadManager().shutdownTask(timeoutFuture);
            timeoutFuture = null;
        }
    }
}

