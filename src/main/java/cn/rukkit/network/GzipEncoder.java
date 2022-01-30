/*
 *  All Rights Reserved.
 *  FileName: GzipEncoder.java
 *  @author: wtbdev
 *  @date: 2022/1/30 下午4:37
 */

package cn.rukkit.network;

import java.io.*;
import java.util.zip.*;

public class GzipEncoder
{
	/* renamed from: a */
    public GZIPOutputStream gzipStream;

	public String str;

    /* renamed from: b */
    public BufferedOutputStream bufferedStream;

    /* renamed from: c */
    public String f4919c;

    /* renamed from: d */
    public ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    /* renamed from: e */
    public DataOutputStream stream;

    /* renamed from: f */
    public boolean f4922f = false;

    /* renamed from: a */
    public final void flush() throws IOException {
        this.buffer.flush();
        if (this.bufferedStream != null) {
            this.bufferedStream.flush();
        }
        if (this.gzipStream != null) {
            this.gzipStream.finish();
        }
    }

    public GzipEncoder(boolean z) throws IOException {
        OutputStream outputStream;
        if (z) {
            this.gzipStream = new GZIPOutputStream(this.buffer);
            this.bufferedStream = new BufferedOutputStream(this.gzipStream);
            outputStream = this.bufferedStream;
        } else {
            outputStream = this.buffer;
        }
        this.stream = new DataOutputStream(outputStream);
    }
}
