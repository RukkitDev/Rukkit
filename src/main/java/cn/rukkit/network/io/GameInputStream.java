/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.io;

import cn.rukkit.network.core.packet.Packet;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.LinkedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data reader for the game's big-endian primitive and block format.
 *
 * <p>A block is encoded as a modified-UTF name, a four-byte length and the
 * block bytes. Compressed blocks contain GZIP data. The reader keeps the
 * stream fields public for compatibility with the existing packet handlers.</p>
 */
public class GameInputStream {
    public static final int DEFAULT_MAX_BLOCK_LENGTH = 16 * 1024 * 1024;

    public final ByteArrayInputStream buffer;
    public final DataInputStream CurrentStream;
    public DataInputStream stream;
    public final LinkedList<DataInputStream> blockQuere = new LinkedList<>();

    private static final Logger LOG = LoggerFactory.getLogger(GameInputStream.class);
    private final int maxBlockLength;

    public GameInputStream(Packet packet) {
        this(requirePayload(packet));
    }

    public GameInputStream(byte[] bytes) {
        this(bytes, DEFAULT_MAX_BLOCK_LENGTH);
    }

    public GameInputStream(byte[] bytes, int maxBlockLength) {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes must not be null");
        }
        validateMaxBlockLength(maxBlockLength);
        this.maxBlockLength = maxBlockLength;
        this.buffer = new ByteArrayInputStream(bytes);
        this.CurrentStream = new DataInputStream(this.buffer);
        this.stream = this.CurrentStream;
    }

    public GameInputStream(DataInputStream stream) {
        if (stream == null) {
            throw new IllegalArgumentException("stream must not be null");
        }
        this.maxBlockLength = DEFAULT_MAX_BLOCK_LENGTH;
        this.buffer = null;
        this.CurrentStream = stream;
        this.stream = stream;
    }

    private static byte[] requirePayload(Packet packet) {
        if (packet == null || packet.bytes == null) {
            throw new IllegalArgumentException("packet payload must not be null");
        }
        return packet.bytes;
    }

    private static void validateMaxBlockLength(int maxBlockLength) {
        if (maxBlockLength <= 0) {
            throw new IllegalArgumentException("maxBlockLength must be positive");
        }
    }

    public short readShort() throws IOException {
        return stream.readShort();
    }

    public byte readByte() throws IOException {
        return stream.readByte();
    }

    public boolean readBoolean() throws IOException {
        return stream.readBoolean();
    }

    public int readInt() throws IOException {
        return stream.readInt();
    }

    public float readFloat() throws IOException {
        return stream.readFloat();
    }

    public long readLong() throws IOException {
        return stream.readLong();
    }

    public String readIsString() throws IOException {
        return readBoolean() ? readString() : "";
    }

    public String readString() throws IOException {
        return stream.readUTF();
    }

    public byte[] readStreamBytes() throws IOException {
        int length = readInt();
        if (length < 0 || length > maxBlockLength) {
            throw new IOException("invalid block length: " + length);
        }
        byte[] bytes = new byte[length];
        stream.readFully(bytes);
        return bytes;
    }

    public void skip(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative");
        }
        try {
            stream.skipBytes(count);
        } catch (IOException e) {
            throw new IllegalStateException("failed to skip bytes", e);
        }
    }

    public DataInputStream getUnDecodeStream() throws IOException {
        readString();
        return new DataInputStream(new ByteArrayInputStream(readStreamBytes()));
    }

    public DataInputStream getDecodeStream() throws IOException {
        readString();
        return new GzipDecoder(readStreamBytes()).stream;
    }

    /** Reads a named block and returns its uncompressed bytes. */
    public byte[] getDecodeBytes() throws IOException {
        readString();
        return readStreamBytes();
    }

    public <T extends Enum<T>> T readEnum(Class<T> enumClass) throws IOException {
        int ordinal = readInt();
        T[] constants = enumClass.getEnumConstants();
        if (ordinal < 0 || ordinal >= constants.length) {
            throw new IOException("invalid enum ordinal: " + ordinal);
        }
        return constants[ordinal];
    }

    public boolean readMark() throws IOException {
        short mark = readShort();
        if (mark != 12345) {
            LOG.error("Failed to readMark: {} != 12345", mark);
            return false;
        }
        return true;
    }

    /** Starts reading a nested block from the current stream. */
    public void startBlock(boolean compressed) throws IOException {
        DataInputStream parent = stream;
        DataInputStream block = compressed ? getDecodeStream() : getUnDecodeStream();
        blockQuere.addLast(parent);
        stream = block;
    }

    /** Returns to the stream that contained the most recently opened block. */
    public void endBlock() {
        if (blockQuere.isEmpty()) {
            throw new IllegalStateException("no open block");
        }
        stream = blockQuere.removeLast();
    }
}
