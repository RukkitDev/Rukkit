package io.rukkit.net;
import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.handler.codec.*;
import io.rukkit.util.*;
import java.io.*;
import java.util.*;

public class PacketDecoder extends ByteToMessageDecoder
{

	private static final int HEAD_SIZE = 8;
	private static Logger log = new Logger("Decoder");
	
	@Override
	protected void decode(ChannelHandlerContext p1, ByteBuf p2, List<Object> p3) throws Exception
	{
		p2.markReaderIndex();
		log.setTag(p1.name());
		
		//log.d("got!");
		
		// 判断是否可读
		if(!p2.isReadable()){
			p2.resetReaderIndex();
			log.d("unreadable!");
			return;
		}
		
		// 判断请求大小
		if(p2.readableBytes() < HEAD_SIZE){
			p2.resetReaderIndex();
			//log.d("too small!");
			return;
		}
		
		// 拼包逻辑
		int length = p2.readInt();
		int type = p2.readInt();
		//log.d(length);
		int readableBytes = p2.readableBytes();
		//log.d(readableBytes);
		//如果可读字节不足
		if(readableBytes < length){
			p2.resetReaderIndex();
			//log.d("not enough!");
			return;
		}
		
		byte bytes[] = new byte[length];
		p2.readBytes(bytes);
		
		Packet p = new Packet(type);
		p.bytes = bytes;
		log.d("Got the packet. (type=" + p.type + " size=" + p.bytes.length + ")");
		
		p3.add(p);
	}
	
}
