/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.core.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.TooLongFrameException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Decodes the length-prefixed packet wire format. */
public class PacketDecoder extends ByteToMessageDecoder {
    private static final int HEADER_SIZE = Integer.BYTES * 2;
    private static final Logger LOG = LoggerFactory.getLogger(PacketDecoder.class);

    private final int maxFrameLength;

    public PacketDecoder() {
        this(Packet.DEFAULT_MAX_FRAME_LENGTH);
    }

    public PacketDecoder(int maxFrameLength) {
        if (maxFrameLength <= 0) {
            throw new IllegalArgumentException("maxFrameLength must be positive");
        }
        this.maxFrameLength = maxFrameLength;
    }

    public int getMaxFrameLength() {
        return maxFrameLength;
    }

    @Override
    protected void decode(ChannelHandlerContext context, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < HEADER_SIZE) {
            return;
        }

        in.markReaderIndex();
        int length = in.readInt();
        int type = in.readInt();

        if (length < 0) {
            throw new CorruptedFrameException("negative packet payload length: " + length);
        }
        if (length > maxFrameLength) {
            throw new TooLongFrameException(
                    "packet payload length " + length + " exceeds " + maxFrameLength);
        }
        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }

        byte[] bytes = new byte[length];
        in.readBytes(bytes);

        Packet packet = new Packet(type, bytes);
        if (LOG.isTraceEnabled() && (type != PacketType.TICK || length > 20)) {
            LOG.trace("Received packet type={} size={}", type, length);
        }
        out.add(packet);
    }
}
