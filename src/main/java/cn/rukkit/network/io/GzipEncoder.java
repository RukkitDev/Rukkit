/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.io;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Collects a protocol block in memory, optionally applying GZIP compression.
 * The public fields are retained for compatibility with the existing packet
 * builders, which write directly to {@link #stream}.
 */
public class GzipEncoder {
    public final GZIPOutputStream gzipStream;
    public final BufferedOutputStream bufferedStream;
    public final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    public final DataOutputStream stream;
    public String str;

    private boolean finished;

    public GzipEncoder(boolean compressed) throws IOException {
        OutputStream output;
        if (compressed) {
            gzipStream = new GZIPOutputStream(buffer);
            bufferedStream = new BufferedOutputStream(gzipStream);
            output = bufferedStream;
        } else {
            gzipStream = null;
            bufferedStream = null;
            output = buffer;
        }
        stream = new DataOutputStream(output);
    }

    /** Flushes all data and finishes the GZIP stream when compression is used. */
    public void flush() throws IOException {
        if (finished) {
            return;
        }
        stream.flush();
        if (bufferedStream != null) {
            bufferedStream.flush();
        }
        if (gzipStream != null) {
            gzipStream.finish();
        }
        finished = true;
    }
}
