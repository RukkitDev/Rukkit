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
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Encodes a {@link Packet} as length, type and payload. */
public class PacketEncoder extends MessageToByteEncoder<Packet> {
    private static final Logger LOG = LoggerFactory.getLogger(PacketEncoder.class);

    private final int maxFrameLength;

    public PacketEncoder() {
        this(Packet.DEFAULT_MAX_FRAME_LENGTH);
    }

    public PacketEncoder(int maxFrameLength) {
        if (maxFrameLength <= 0) {
            throw new IllegalArgumentException("maxFrameLength must be positive");
        }
        this.maxFrameLength = maxFrameLength;
    }

    public int getMaxFrameLength() {
        return maxFrameLength;
    }

    @Override
    protected void encode(ChannelHandlerContext context, Packet packet, ByteBuf out) {
        if (packet == null) {
            throw new EncoderException("packet must not be null");
        }
        if (packet.bytes == null) {
            throw new EncoderException("packet payload must not be null");
        }
        if (packet.bytes.length > maxFrameLength) {
            throw new EncoderException("packet payload exceeds maxFrameLength: " + packet.bytes.length);
        }

        if (LOG.isTraceEnabled() && (packet.type != PacketType.TICK || packet.bytes.length > 20)) {
            LOG.trace("Sending packet type={} size={}", packet.type, packet.bytes.length);
        }

        out.writeInt(packet.bytes.length);
        out.writeInt(packet.type);
        out.writeBytes(packet.bytes);
    }
}
