/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.core.packet;

import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.handler.codec.*;
import org.slf4j.*;


public class PacketEncoder extends MessageToByteEncoder<Packet>
{

	Logger log = LoggerFactory.getLogger(PacketEncoder.class);
	@Override
	protected void encode(ChannelHandlerContext p1, Packet p2, ByteBuf p3) throws Exception
	{
		// TODO: Implement this method
		//log.setTag(p1.name());
		if (p2.type != 10) {
			log.trace("Sending packets... (type=" + p2.type + " size=" + p2.bytes.length + ")");
		} else {
			if (p2.bytes.length > 20) {
				log.trace("Sending game packets... (type=" + p2.type + " size=" + p2.bytes.length + ")");
			}
		}
		p3.writeInt(p2.bytes.length);
		p3.writeInt(p2.type);
		p3.writeBytes(p2.bytes);
	}

}
