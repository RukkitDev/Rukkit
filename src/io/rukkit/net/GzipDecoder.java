package io.rukkit.net;

import java.io.*;
import java.util.zip.*;

public class GzipDecoder
{
	//public String str;
    public ByteArrayInputStream buffer;
    public DataInputStream stream;

    public GzipDecoder(byte[] bytes) throws IOException {
        this.buffer = new ByteArrayInputStream(bytes);
        BufferedInputStream in = new BufferedInputStream((InputStream)new GZIPInputStream((InputStream)this.buffer));
        this.stream = new DataInputStream((InputStream)in);
    }
}
