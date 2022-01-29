package cn.rukkit.network.packet;

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
			log.debug("Sending packets... (type=" + p2.type + " size=" + p2.bytes.length + ")");
		}
		p3.writeInt(p2.bytes.length);
		p3.writeInt(p2.type);
		p3.writeBytes(p2.bytes);
	}

}
