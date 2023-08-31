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
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class GameOutputStream
{
	public ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	public DataOutputStream stream = new DataOutputStream(buffer);
	public DataOutputStream currentStream = new DataOutputStream(buffer);
	public LinkedList<GzipEncoder> blockQuere = new LinkedList<GzipEncoder>();

	private GZIPOutputStream gzipStream;

	private BufferedOutputStream bufferedStream;
	
	private ByteArrayOutputStream buff = new ByteArrayOutputStream();
	
	public Packet createPacket(int type) {
        try {
			while (blockQuere.size() != 0) {
				endBlock();
			}
            this.stream.flush();
            this.buffer.flush();
            Packet packet = new Packet(type);
            packet.bytes = this.buffer.toByteArray();
            return packet;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeByte(int val) throws IOException {
        this.stream.writeByte(val);
    }

    public void writeBoolean(boolean val) throws IOException {
        this.stream.writeBoolean(val);
    }

    public void writeInt(int val) throws IOException {
        this.stream.writeInt(val);
    }

    public void writeFloat(float val) throws IOException {
        this.stream.writeFloat(val);
    }

    public void writeLong(long val) throws IOException {
        this.stream.writeLong(val);
    }

	public void writeShort(short val) throws IOException {
		this.stream.writeShort(val);
	}

    public void writeString(String val) throws IOException {
        this.stream.writeUTF(val);
    }
	
	public void write(byte[] val) throws IOException {
		this.stream.write(val);
		this.stream.flush();
	}

	public GzipEncoder getEncodeStream(String key, boolean isGzip) throws IOException{
		GzipEncoder enc = new GzipEncoder(isGzip);
		enc.str = key;
		return enc;
	}

	public void writeFile(FileInputStream stream) throws IOException{
		byte[] filearr = new byte[stream.available()];
		int bytesRead = 0;
		while (bytesRead < filearr.length)
		{
			int readIn = stream.read(filearr, bytesRead, filearr.length - bytesRead);
			if (readIn == -1)
			{
				break;
			}
			bytesRead += readIn;
		}
		this.stream.writeInt(filearr.length);
		this.stream.write(filearr);
	}

	public void flushEncodeData(GzipEncoder enc) throws IOException{
		enc.flush();
		this.writeString(enc.str);
		this.writeInt(enc.buffer.size());
		enc.buffer.writeTo((OutputStream)this.stream);
		//stream.flush();
	}

	public void writeEnum(Enum clazz) throws IOException {
		this.writeInt(clazz.ordinal());
		//return (Enum)clazz.getEnumConstants()[i];
	}
	
	/*
	* Start to writing a content block.
	* @params blockName a block content name
	*/
	public void startBlock(String blockName, boolean isGzip) throws IOException {
		GzipEncoder enc = getEncodeStream(blockName, isGzip);
		currentStream = stream;
		stream = enc.stream;
		blockQuere.addLast(enc);
		OutputStream outputStream;
		if (isGzip) {
			this.gzipStream = new GZIPOutputStream(this.buff);
			this.bufferedStream = new BufferedOutputStream(this.gzipStream);
			outputStream = this.bufferedStream;
		} else {
			outputStream = this.buff;
		}
		stream = new DataOutputStream(outputStream);
	}
	
	/*
	* End a content block.
	*/
	public void endBlock() throws IOException {
		if (blockQuere.size() != 0) {
			GzipEncoder enc = blockQuere.removeLast();
			//enc.stream = stream;
			//enc.stream.write(stream.
			currentStream.writeUTF(enc.str);
			currentStream.writeInt(stream.size());
			buff.writeTo((OutputStream)this.currentStream);
			buff.flush();
			//detect next block
			if (blockQuere.size() == 0) {
				stream = currentStream;
			} else {
				stream = blockQuere.getLast().stream;
			}
		}
		
	}
}
