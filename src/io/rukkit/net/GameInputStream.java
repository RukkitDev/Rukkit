package io.rukkit.net;

import java.io.*;

public class GameInputStream
{
	public ByteArrayInputStream buffer;
    public DataInputStream stream;

    public GameInputStream(Packet packet) {
        this.buffer = new ByteArrayInputStream(packet.bytes);
		this.stream = new DataInputStream(this.buffer);
    }
	
	public GameInputStream(byte[] b){
		this.buffer = new ByteArrayInputStream(b);
		this.stream = new DataInputStream(this.buffer);
	}
	
	public GameInputStream(DataInputStream stream){
		//this.buffer = new ByteArrayInputStream(b);
		this.stream = stream;
	}

	public DataInputStream getUnDecodeStream() throws IOException
	{
		this.readString();
		byte[] bytes = readStreamBytes();
		return new DataInputStream(new ByteArrayInputStream(bytes));
		// TODO: Implement this method
		//return null;
	}

	public short readShort() throws IOException {
		return this.stream.readShort();
	}

    public int readByte() throws IOException {
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
		this.readString();
		byte[] bytes = readStreamBytes();
		GzipDecoder coder = new GzipDecoder(bytes);
		return coder.stream;
	}

	public byte[] getDecodeBytes() throws IOException{
		this.readString();
		byte[] bytes = readStreamBytes();
		return bytes;
	}
	
	public Enum readEnum(Class clazz) throws IOException{
		int i = this.readInt();
		return (Enum)clazz.getEnumConstants()[i];
	}
}
