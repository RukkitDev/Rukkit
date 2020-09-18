package io.rukkit.net;
import io.netty.handler.codec.*;
import io.netty.buffer.*;
import io.netty.channel.*;
import io.rukkit.util.*;

public class PacketEncoder extends MessageToByteEncoder<Packet>
{

	Logger log = new Logger("Encoder");
	@Override
	protected void encode(ChannelHandlerContext p1, Packet p2, ByteBuf p3) throws Exception
	{
		// TODO: Implement this method
		log.setTag(p1.name());
		//log.d("Sending packets... (type=" + p2.type + " size=" + p2.bytes.length + ")");
		p3.writeInt(p2.bytes.length);
		p3.writeInt(p2.type);
		p3.writeBytes(p2.bytes);
	}
	
}
