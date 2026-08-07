/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.core.handler;

import cn.rukkit.Rukkit;
import cn.rukkit.event.player.PlayerLeftEvent;
import cn.rukkit.network.ConnectionState;
import cn.rukkit.network.room.ServerRoom;
import cn.rukkit.network.room.ServerRoomConnection;
import cn.rukkit.network.core.packet.Packet;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

/**
 * Connection lifecycle handler for the core packet pipeline.
 *
 * <p>The global connection cleanup callback is injected so this class does
 * not depend on the legacy global manager while the two runtimes coexist.</p>
 */
public class ServerConnectionHandler extends ChannelInboundHandlerAdapter {
    private final Logger log = LoggerFactory.getLogger(ServerConnectionHandler.class);
    private final ServerPacketHandlerManager packetHandlerManager;
    private final Consumer<ServerRoomConnection> globalDiscard;

    public ChannelHandlerContext ctx;
    private ServerRoomConnection conn;
    private ConnectionState state = ConnectionState.CONNECTED;
    private ScheduledFuture<?> timeoutFuture;
    private ServerRoom currentRoom;
    private String disconnectReason = "Unknown";

    public ServerConnectionHandler(ServerPacketHandlerManager packetHandlerManager) {
        this(packetHandlerManager, connection -> {
        });
    }

    public ServerConnectionHandler(ServerPacketHandlerManager packetHandlerManager,
                                   Consumer<ServerRoomConnection> globalDiscard) {
        this.packetHandlerManager = Objects.requireNonNull(packetHandlerManager,
                "packetHandlerManager must not be null");
        this.globalDiscard = Objects.requireNonNull(globalDiscard,
                "globalDiscard must not be null");
    }

    public class TimeoutTask implements Runnable {
        private int execTime;

        @Override
        public void run() {
            execTime++;
            if (execTime >= Rukkit.getConfig().registerTimeout && ctx != null) {
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
            PlayerLeftEvent.getListenerList().callListeners(
                    new PlayerLeftEvent(conn.player, disconnectReason));
            if (currentRoom != null && currentRoom.connectionManager != null) {
                currentRoom.connectionManager.discard(conn);
            }
            globalDiscard.accept(conn);
            conn.stopPingTask();
            conn.stopTeamTask();
        } else {
            log.debug("Unregistered connection closed from {}", ctx.channel().remoteAddress());
        }
        stopTimeout();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (!(msg instanceof Packet packet)) {
                log.debug("Ignoring non-core packet {} from {}", msg.getClass().getName(),
                        ctx.channel().remoteAddress());
                return;
            }
            if (!packetHandlerManager.dispatch(new ServerPacketContext(ctx, conn, this), packet)) {
                log.debug("Unhandled packet type {} from {}", packet.type,
                        ctx.channel().remoteAddress());
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
        this.state = Objects.requireNonNull(state, "state must not be null");
    }

    public ServerRoomConnection getConn() {
        return conn;
    }

    public void setConn(ServerRoomConnection conn) {
        this.conn = conn;
        this.currentRoom = conn == null ? null : conn.currectRoom;
    }

    public ServerRoom getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(ServerRoom currentRoom) {
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
