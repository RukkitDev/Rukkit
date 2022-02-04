/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
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
