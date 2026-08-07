package cn.rukkit.network.core.packet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.TooLongFrameException;
import org.junit.jupiter.api.Test;

class PacketCodecTest {
    @Test
    void encoderWritesLengthTypeAndPayload() {
        EmbeddedChannel channel = new EmbeddedChannel(new PacketEncoder(16));

        assertTrue(channel.writeOutbound(new Packet(7, new byte[] {1, 2, 3})));
        ByteBuf encoded = channel.readOutbound();
        assertEquals(3, encoded.readInt());
        assertEquals(7, encoded.readInt());
        byte[] payload = new byte[3];
        encoded.readBytes(payload);
        assertArrayEquals(new byte[] {1, 2, 3}, payload);
        assertFalse(encoded.isReadable());
        encoded.release();
        channel.finishAndReleaseAll();
    }

    @Test
    void defaultCodecDoesNotLimitPayloadLength() {
        byte[] payload = new byte[8193];
        EmbeddedChannel encoder = new EmbeddedChannel(new PacketEncoder());

        assertTrue(encoder.writeOutbound(new Packet(7, payload)));
        ByteBuf encoded = encoder.readOutbound();
        assertEquals(payload.length, encoded.readInt());
        encoded.skipBytes(Integer.BYTES);
        encoded.release();
        encoder.finishAndReleaseAll();

        EmbeddedChannel decoder = new EmbeddedChannel(new PacketDecoder());
        ByteBuf frame = Unpooled.buffer().writeInt(payload.length).writeInt(7).writeBytes(payload);
        assertTrue(decoder.writeInbound(frame));
        Packet decoded = decoder.readInbound();
        assertEquals(payload.length, decoded.bytes.length);
        decoder.finishAndReleaseAll();
    }

    @Test
    void decoderHandlesFragmentedFrames() {
        EmbeddedChannel channel = new EmbeddedChannel(new PacketDecoder(16));
        ByteBuf first = Unpooled.buffer();
        first.writeInt(4).writeInt(9).writeBytes(new byte[] {1, 2});

        assertFalse(channel.writeInbound(first));
        assertNull(channel.readInbound());

        ByteBuf second = Unpooled.buffer().writeBytes(new byte[] {3, 4});
        assertTrue(channel.writeInbound(second));
        Packet decoded = channel.readInbound();
        assertEquals(9, decoded.type);
        assertArrayEquals(new byte[] {1, 2, 3, 4}, decoded.bytes);
        channel.finishAndReleaseAll();
    }

    @Test
    void decoderRejectsNegativeLength() {
        EmbeddedChannel channel = new EmbeddedChannel(new PacketDecoder(16));
        ByteBuf frame = Unpooled.buffer().writeInt(-1).writeInt(9);

        assertThrows(Exception.class, () -> channel.writeInbound(frame));
        channel.finishAndReleaseAll();
    }

    @Test
    void decoderRejectsOversizedLengthBeforeAllocation() {
        EmbeddedChannel channel = new EmbeddedChannel(new PacketDecoder(16));
        ByteBuf frame = Unpooled.buffer().writeInt(17).writeInt(9);

        assertThrows(TooLongFrameException.class, () -> channel.writeInbound(frame));
        channel.finishAndReleaseAll();
    }

    @Test
    void encoderRejectsOversizedPayload() {
        EmbeddedChannel channel = new EmbeddedChannel(new PacketEncoder(2));

        assertThrows(EncoderException.class,
                () -> channel.writeOutbound(new Packet(9, new byte[] {1, 2, 3})));
        channel.finishAndReleaseAll();
    }
}
