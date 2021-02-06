package io.rukkit.net;

import java.io.*;

public class GameOutputStream
{
	ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	DataOutputStream stream = new DataOutputStream(buffer);
	public Packet createPacket(int type) {
        try {
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

    public void writeString(String val) throws IOException {
        this.stream.writeUTF(val);
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
		enc.mo1925a();
		this.writeString(enc.str);
		this.writeInt(enc.buffer.size());
		enc.buffer.writeTo((OutputStream)this.stream);
		//stream.flush();
	}
}
