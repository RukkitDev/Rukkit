/*
 * Copyright 2025 Micro(MCLDY@outlook.com) and contributors.
 * 
 * 本衍生作品基于 AGPLv3 许可证
 * This derivative work is licensed under AGPLv3
 */
package cn.rukkit.network.core;

import io.netty.channel.ChannelHandlerContext;

public abstract class PacketHandler {
    public abstract void handle() throws Exception;
    public abstract void updateMsg(ChannelHandlerContext ctx,Object msg);
	public abstract void onConnectionClose(ChannelHandlerContext ctx);
}