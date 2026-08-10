/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.core.handler;

import cn.rukkit.network.room.ServerGlobalConnectionManager;
import cn.rukkit.network.room.ServerRoomManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/** Dispatches core-layer packets while enforcing connection state guards. */
public class ServerPacketHandlerManager {
    private static final Logger log = LoggerFactory.getLogger(ServerPacketHandlerManager.class);
    private final Map<Integer, ServerPacketHandler> handlers = new HashMap<>();

    public void register(ServerPacketHandler handler) {
        handlers.put(handler.getType(), handler);
    }

    public void unregister(int type) {
        handlers.remove(type);
    }

    public void unregister(ServerPacketHandler handler) {
        unregister(handler.getType());
    }

    /**
     * Register the core handlers that have already been migrated from master.
     *
     * <p>This registry is intentionally separate from the legacy packet
     * handler manager while both network runtimes coexist.</p>
     */
    public void registerInternalHandler(ServerRoomManager roomManager,
                                         ServerGlobalConnectionManager globalConnectionManager) {
        registerInternalHandler(roomManager, globalConnectionManager, null);
    }

    /**
     * Register migrated handlers with an optional new-stack chat command
     * dispatcher. The legacy command manager remains outside this registry.
     */
    public void registerInternalHandler(ServerRoomManager roomManager,
                                         ServerGlobalConnectionManager globalConnectionManager,
                                         ServerChatCommandDispatcher commandDispatcher) {
        register(new ServerPreRegisterHandler());
        register(new ServerPlayerInfoHandler(roomManager, globalConnectionManager));
        register(new ServerHeartbeatResponseHandler());
        register(new ServerDisconnectHandler());
        register(new ServerQuestionResponseHandler());
        register(new ServerRandyHandler());
        register(new ServerAddGameCommandHandler());
        register(new ServerSyncHandler());
        register(new ServerSyncChecksumResponseHandler());
        if (commandDispatcher == null) {
            register(new ServerAddChatHandler());
        } else {
            register(new ServerAddChatHandler(commandDispatcher));
        }
    }

    public boolean dispatch(ServerPacketContext context,
                            cn.rukkit.network.core.packet.Packet packet) throws Exception {
        ServerPacketHandler handler = handlers.get(packet.type);
        if (handler == null) {
            return false;
        }
        if (!handler.getAllowedStates().contains(context.state())) {
            log.warn("Packet {} blocked by state {} from {}", packet.type, context.state(),
                    context.ctx().channel().remoteAddress());
            context.ctx().close();
            return true;
        }
        handler.handle(context, packet);
        return true;
    }
}
