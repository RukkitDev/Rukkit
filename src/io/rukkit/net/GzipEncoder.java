package io.rukkit.net;

import java.io.*;
import java.util.zip.*;

public class GzipEncoder
{
	/* renamed from: a */
    public GZIPOutputStream f4917a;
	
	public String str;

    /* renamed from: b */
    public BufferedOutputStream f4918b;

    /* renamed from: c */
    public String f4919c;

    /* renamed from: d */
    public ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    /* renamed from: e */
    public DataOutputStream stream;

    /* renamed from: f */
    public boolean f4922f = false;

    /* renamed from: a */
    public final void mo1925a() throws IOException {
        this.buffer.flush();
        if (this.f4918b != null) {
            this.f4918b.flush();
        }
        if (this.f4917a != null) {
            this.f4917a.finish();
        }
    }

    public GzipEncoder(boolean z) throws IOException {
        OutputStream outputStream;
        if (z) {
            this.f4917a = new GZIPOutputStream(this.buffer);
            this.f4918b = new BufferedOutputStream(this.f4917a);
            outputStream = this.f4918b;
        } else {
            outputStream = this.buffer;
        }
        this.stream = new DataOutputStream(outputStream);
    }
	/*public String str;
    public ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	public BufferedOutputStream outstream;
    public DataOutputStream stream;
	public GZIPOutputStream gzipout;

    public GzipEncoder(boolean isGzip) throws IOException{
		if (isGzip) {
			gzipout = new GZIPOutputStream(buffer);
			outstream = new BufferedOutputStream(gzipout);
			this.stream = new DataOutputStream(outstream);
			return;
		}
        this.buffer = new ByteArrayOutputStream();
        this.stream = new DataOutputStream((OutputStream)this.buffer);
    }*/
}
