/*
 *  All Rights Reserved.
 *  FileName: PacketDecoder.java
 *  @author: wtbdev
 *  @date: 2022/1/30 下午4:37
 */

package cn.rukkit.network.packet;

import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.handler.codec.*;
import java.util.*;
import org.slf4j.*;

public class PacketDecoder extends ByteToMessageDecoder
{

	private static final int HEAD_SIZE = 8;
	private static Logger log = LoggerFactory.getLogger(PacketDecoder.class);

	@Override
	protected void decode(ChannelHandlerContext p1, ByteBuf p2, List<Object> p3) throws Exception
	{
		p2.markReaderIndex();
		//log.(p1.name());

		//log.d("got!");

		// 判断是否可读
		if(!p2.isReadable()){
			p2.resetReaderIndex();
			log.debug("unreadable!");
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
		log.debug("Got the packet. (type=" + p.type + " size=" + p.bytes.length + ")");

		p3.add(p);
	}
}

