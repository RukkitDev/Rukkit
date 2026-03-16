/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.packet.handler;

import cn.rukkit.network.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class PacketHandlerManager {
    private static final Logger log = LoggerFactory.getLogger(PacketHandlerManager.class);
    private final Map<Integer, PacketHandler> handlers = new HashMap<>();
    public void register(PacketHandler handler) {
        handlers.put(handler.getType(), handler);
    }

    public void unregister(int type) { handlers.remove(type); }
    public void unregister(PacketHandler handler) {
        handlers.remove(handler.getType());
    }

    /**
     * 处理 Packet
     * @param ctx Packet 上下文
     * @param packet 要处理的 Packet
     * @throws Exception 处理过程中发生的异常
     */
    public boolean dispatch(PacketContext ctx, Packet packet) throws Exception {
        PacketHandler h = handlers.get(packet.type);
        if (h == null) return false;
        if (!h.getAllowedStates().contains(ctx.state())) {
            log.warn("Packet {} blocked by state {} from {}", packet.type, ctx.state(), ctx.ctx().channel().remoteAddress());
            ctx.ctx().close();
            return true;
        }
        h.handle(ctx, packet);
        return true;
    }

    public void registerInternalHandler() {
        register(new PreRegisterHandler());
        register(new PlayerInfoHandler());
        register(new HeartbeatResponseHandler());
        register(new AddChatHandler());
        register(new AddGameCommandHandler());
        register(new RandyHandler());
        register(new SyncHandler());
        register(new SyncChecksumResponseHandler());
        register(new DisconnectHandler());
        register(new QuestionResponseHandler());
    }
}
