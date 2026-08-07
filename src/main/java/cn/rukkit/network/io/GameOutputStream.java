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
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;

/** Data writer for the game's big-endian primitive and block format. */
public class GameOutputStream {
    public final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    public DataOutputStream stream;
    public DataOutputStream currentStream;
    public final LinkedList<GzipEncoder> blockQuere = new LinkedList<>();

    private final LinkedList<DataOutputStream> parentStreams = new LinkedList<>();

    public GameOutputStream() {
        stream = new DataOutputStream(buffer);
        currentStream = stream;
    }

    public Packet createPacket(int type) {
        try {
            while (!blockQuere.isEmpty()) {
                endBlock();
            }
            stream.flush();
            buffer.flush();
            return new Packet(type, buffer.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("failed to create packet", e);
        }
    }

    public void writeByte(int value) throws IOException {
        stream.writeByte(value);
    }

    public void writeBoolean(boolean value) throws IOException {
        stream.writeBoolean(value);
    }

    public void writeInt(int value) throws IOException {
        stream.writeInt(value);
    }

    public void writeFloat(float value) throws IOException {
        stream.writeFloat(value);
    }

    public void writeLong(long value) throws IOException {
        stream.writeLong(value);
    }

    public void writeShort(short value) throws IOException {
        stream.writeShort(value);
    }

    public void writeIsString(String value) throws IOException {
        if (value == null || value.trim().isEmpty()) {
            writeBoolean(false);
        } else {
            writeBoolean(true);
            writeString(value);
        }
    }

    public void writeString(String value) throws IOException {
        stream.writeUTF(value);
    }

    public void write(byte[] value) throws IOException {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        stream.write(value);
    }

    public GzipEncoder getEncodeStream(String key, boolean compressed) throws IOException {
        GzipEncoder encoder = new GzipEncoder(compressed);
        encoder.str = key;
        return encoder;
    }

    public void writeFile(FileInputStream input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }
        ByteArrayOutputStream fileBytes = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = input.read(chunk)) != -1) {
            fileBytes.write(chunk, 0, read);
        }
        byte[] bytes = fileBytes.toByteArray();
        writeInt(bytes.length);
        write(bytes);
    }

    /** Writes a separately-created named block to the current stream. */
    public void flushEncodeData(GzipEncoder encoder) throws IOException {
        if (encoder == null) {
            throw new IllegalArgumentException("encoder must not be null");
        }
        encoder.flush();
        stream.writeUTF(encoder.str);
        stream.writeInt(encoder.buffer.size());
        encoder.buffer.writeTo(stream);
    }

    public void writeEnum(Enum<?> value) throws IOException {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        writeInt(value.ordinal());
    }

    /** Starts a named block. Its contents are isolated until {@link #endBlock()}. */
    public void startBlock(String blockName, boolean compressed) throws IOException {
        if (blockName == null) {
            throw new IllegalArgumentException("blockName must not be null");
        }
        GzipEncoder encoder = getEncodeStream(blockName, compressed);
        parentStreams.addLast(stream);
        blockQuere.addLast(encoder);
        currentStream = stream;
        stream = encoder.stream;
    }

    /** Finishes the most recent block and appends it to its parent stream. */
    public void endBlock() throws IOException {
        if (blockQuere.isEmpty() || parentStreams.isEmpty()) {
            throw new IllegalStateException("no open block");
        }

        GzipEncoder encoder = blockQuere.removeLast();
        DataOutputStream parent = parentStreams.removeLast();
        encoder.flush();
        parent.writeUTF(encoder.str);
        parent.writeInt(encoder.buffer.size());
        encoder.buffer.writeTo(parent);
        parent.flush();
        stream = parent;
        currentStream = parent;
    }
}
