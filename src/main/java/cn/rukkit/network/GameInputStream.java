/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network;

import cn.rukkit.network.packet.*;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class GameInputStream
{
	public ByteArrayInputStream buffer;
    public DataInputStream CurrentStream;
	public DataInputStream stream;
	public LinkedList<DataInputStream> blockQuere = new LinkedList<DataInputStream>();

    public GameInputStream(Packet packet) {
        this.buffer = new ByteArrayInputStream(packet.bytes);
		this.stream = new DataInputStream(this.buffer);
		this.CurrentStream = new DataInputStream(this.buffer);
    }

	public GameInputStream(byte[] b){
		this.buffer = new ByteArrayInputStream(b);
		this.stream = new DataInputStream(this.buffer);
		this.CurrentStream = new DataInputStream(this.buffer);
	}

	public GameInputStream(DataInputStream stream){
		//this.buffer = new ByteArrayInputStream(b);
		this.stream = stream;
	}

	public DataInputStream getUnDecodeStream() throws IOException
	{
		String blockName = this.readString();
		// LoggerFactory.getLogger("GameInputStream").info("BlockName: {}", blockName);
		byte[] bytes = readStreamBytes();
		return new DataInputStream(new ByteArrayInputStream(bytes));
		// TODO: Implement this method
		//return null;
	}

	public short readShort() throws IOException {
		return this.stream.readShort();
	}

    public byte readByte() throws IOException {
        return this.stream.readByte();
    }

    public boolean readBoolean() throws IOException {
        return this.stream.readBoolean();
    }

    public int readInt() throws IOException {
        return this.stream.readInt();
    }

    public float readFloat() throws IOException {
        return this.stream.readFloat();
    }

    public long readLong() throws IOException {
        return this.stream.readLong();
    }

    public String readString() throws IOException {
        return this.stream.readUTF();
    }

	public byte[] readStreamBytes() throws IOException {
        int n2;
        int n3 = this.readInt();
        byte[] arrby = new byte[n3];
        for (int i2 = 0; i2 < n3 && (n2 = this.stream.read(arrby, i2, n3 - i2)) != -1; i2 += n2) {
        }
        return arrby;
    }

	public DataInputStream getDecodeStream() throws IOException{
		String blockName = this.readString();
		// LoggerFactory.getLogger("GameInputStream").info("BlockName: {}", blockName);
		byte[] bytes = readStreamBytes();
		GzipDecoder coder = new GzipDecoder(bytes);
		return coder.stream;
	}

	/*
	*  get decoded bytes.
	*
	*/
	public byte[] getDecodeBytes() throws IOException{
		this.readString();
		byte[] bytes = readStreamBytes();
		return bytes;
	}

	public Enum readEnum(Class clazz) throws IOException{
		int i = this.readInt();
		return (Enum)clazz.getEnumConstants()[i];
	}

	public boolean readMark() throws IOException {
		short mark = this.readShort();
		if (mark != 12345) {
			LoggerFactory.getLogger("GameInputStream").error("Failed to readMark: {} != 12345", mark);
			return false;
		} else {
			return true;
		}
	}
	
	/*
	* Starts a block to read content.
	* Block content will buffered to List;
	*/
	public void startBlock(boolean isCompressed) throws IOException {
		if (isCompressed) {
			stream = getDecodeStream();
			blockQuere.add(stream);
		} else {
			stream = getUnDecodeStream();
			blockQuere.add(stream);
		}
	}
	
	/*
	* Ends a block content.
	* Delete the buffer to get the current stream.
	*/
	public void endBlock() {
		blockQuere.removeLast();
		if (blockQuere.size() == 0) {
			stream = CurrentStream;
		} else {
			stream = blockQuere.getLast();
		}
	}
}
