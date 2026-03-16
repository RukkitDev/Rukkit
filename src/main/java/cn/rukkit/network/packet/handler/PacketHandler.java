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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class PacketHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * 获取该处理器处理的 Packet 的类型
     * @return Packet 的类型
     */
    public abstract int getType();

    /**
     * 获取该处理器允许处理的状态
     * @return 允许处理的状态
     */
    public List<ConnectionState> getAllowedStates() {
        return List.of(ConnectionState.values());
    }

    /**
     * 处理 Packet
     * @param ctx Packet 上下文
     * @param packet 要处理的 Packet
     * @throws Exception 处理过程中发生的异常
     */
    public abstract void handle(PacketContext ctx, Packet packet) throws Exception;
    protected Logger getLogger() {
        return logger;
    }
}

