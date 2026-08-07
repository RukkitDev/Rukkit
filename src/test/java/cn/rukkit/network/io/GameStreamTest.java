package cn.rukkit.network.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.rukkit.network.core.packet.Packet;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class GameStreamTest {
    private enum TestAction {
        FIRST,
        SECOND
    }

    @Test
    void roundTripsPrimitiveValues() throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeBoolean(true);
        output.writeByte(3);
        output.writeShort((short) 4);
        output.writeInt(5);
        output.writeLong(6L);
        output.writeFloat(7.5f);
        output.writeString("hello");
        output.writeIsString("");
        output.writeEnum(TestAction.SECOND);

        Packet packet = output.createPacket(1);
        GameInputStream input = new GameInputStream(packet);
        assertTrue(input.readBoolean());
        assertEquals(3, input.readByte());
        assertEquals(4, input.readShort());
        assertEquals(5, input.readInt());
        assertEquals(6L, input.readLong());
        assertEquals(7.5f, input.readFloat());
        assertEquals("hello", input.readString());
        assertEquals("", input.readIsString());
        assertEquals(TestAction.SECOND, input.readEnum(TestAction.class));
    }

    @Test
    void roundTripsUncompressedBlock() throws IOException {
        assertBlockRoundTrip(false);
    }

    @Test
    void roundTripsCompressedBlock() throws IOException {
        assertBlockRoundTrip(true);
    }

    @Test
    void roundTripsNestedBlocks() throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.startBlock("outer", false);
        output.writeInt(1);
        output.startBlock("inner", true);
        output.writeString("nested");
        output.endBlock();
        output.endBlock();

        GameInputStream input = new GameInputStream(output.createPacket(1));
        input.startBlock(false);
        assertEquals(1, input.readInt());
        input.startBlock(true);
        assertEquals("nested", input.readString());
        input.endBlock();
        input.endBlock();
    }

    @Test
    void flushesStandaloneCompressedBlock() throws IOException {
        GameOutputStream output = new GameOutputStream();
        GzipEncoder encoder = output.getEncodeStream("payload", true);
        encoder.stream.writeInt(42);
        output.flushEncodeData(encoder);

        GameInputStream input = new GameInputStream(output.createPacket(1));
        input.startBlock(true);
        assertEquals(42, input.readInt());
        input.endBlock();
    }

    @Test
    void rejectsInvalidBlockLength() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(bytes);
        data.writeUTF("bad");
        data.writeInt(-1);
        data.flush();

        GameInputStream input = new GameInputStream(bytes.toByteArray());
        assertThrows(IOException.class, () -> input.startBlock(false));
    }

    private static void assertBlockRoundTrip(boolean compressed) throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.startBlock("block", compressed);
        output.writeInt(42);
        output.writeString("content");
        output.endBlock();

        GameInputStream input = new GameInputStream(output.createPacket(1));
        input.startBlock(compressed);
        assertEquals(42, input.readInt());
        assertEquals("content", input.readString());
        input.endBlock();
        assertFalse(input.blockQuere.iterator().hasNext());
    }
}
