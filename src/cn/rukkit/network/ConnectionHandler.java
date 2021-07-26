package cn.rukkit.network;
import io.netty.channel.*;
import org.slf4j.*;

public class ConnectionHandler extends ChannelInboundHandlerAdapter
{
	Logger log = LoggerFactory.getLogger(ConnectionHandler.class);

	public ChannelHandlerContext ctx;

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception
	{
		// TODO: Implement this method
		super.channelRegistered(ctx);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception
	{
		// TODO: Implement this method
		super.channelActive(ctx);
		// 保存ctx实例
		this.ctx = ctx;
		
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception
	{
		// TODO: Implement this method
		super.channelInactive(ctx);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
	{
		// TODO: Implement this method
		super.channelRead(ctx, msg);
	}
	
	
}
