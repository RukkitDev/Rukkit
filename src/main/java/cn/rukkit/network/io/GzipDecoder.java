/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.io;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

/** Reads a GZIP-compressed protocol block from memory. */
public class GzipDecoder {
    public final ByteArrayInputStream buffer;
    public final DataInputStream stream;

    public GzipDecoder(byte[] bytes) throws IOException {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes must not be null");
        }
        buffer = new ByteArrayInputStream(bytes);
        stream = new DataInputStream(new BufferedInputStream(new GZIPInputStream(buffer)));
    }
}
