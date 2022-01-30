/*
 *  All Rights Reserved.
 *  FileName: GzipDecoder.java
 *  @author: wtbdev
 *  @date: 2022/1/30 下午4:37
 */

package cn.rukkit.network;

import java.io.*;
import java.util.zip.*;

public class GzipDecoder
{
	public ByteArrayInputStream buffer;
    public DataInputStream stream;

    public GzipDecoder(byte[] bytes) throws IOException {
        this.buffer = new ByteArrayInputStream(bytes);
        BufferedInputStream in = new BufferedInputStream((InputStream)new GZIPInputStream((InputStream)this.buffer));
        this.stream = new DataInputStream((InputStream)in);
    }
}
