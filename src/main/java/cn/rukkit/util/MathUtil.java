/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.util;

public class MathUtil
{
    @SuppressWarnings(value = {"all"})
	public static final /*strictfp*/ int abs(int n2) {
        return n2 < 0 ? -n2 : n2;
    }

    //base64解码
    private static final byte[] BASE64_DECODE_TABLE = new byte[256];
    static {
        for (int i = 0; i < 256; i++) {
            BASE64_DECODE_TABLE[i] = -1;
        }
        for (int i = 65; i <= 90; i++) {
            BASE64_DECODE_TABLE[i] = (byte) (i - 65);
        }
        for (int i = 97; i <= 122; i++) {
            BASE64_DECODE_TABLE[i] = (byte) ((i + 26) - 97);
        }
        for (int i = 48; i <= 57; i++) {
            BASE64_DECODE_TABLE[i] = (byte) ((i + 52) - 48);
        }
        BASE64_DECODE_TABLE[43] = 62;
        BASE64_DECODE_TABLE[47] = 63;
    }

    public static byte[] decodeBase64Custom(String base64Str) {
        char[] charArray = base64Str.toCharArray();
        int length = charArray.length;

        // 计算有效字符长度
        for (int i = 0; i < charArray.length; i++) {
            if (charArray[i] > 255 || BASE64_DECODE_TABLE[charArray[i]] < 0) {
                length--;
            }
        }

        // 计算输出字节长度
        int outputLength = (length / 4) * 3;
        if (length % 4 == 3) {
            outputLength += 2;
        }
        if (length % 4 == 2) {
            outputLength += 1;
        }

        byte[] output = new byte[outputLength];
        int bitBuffer = 0;
        int bitsInBuffer = 0;
        int outputIndex = 0;

        for (int i = 0; i < charArray.length; i++) {
            char c = charArray[i];
            if (c > 255) {
                continue;
            }

            byte decodedByte = BASE64_DECODE_TABLE[c];
            if (decodedByte < 0) {
                continue;
            }

            bitBuffer = (bitBuffer << 6) | decodedByte;
            bitsInBuffer += 6;

            if (bitsInBuffer >= 8) {
                bitsInBuffer -= 8;
                output[outputIndex++] = (byte) ((bitBuffer >> bitsInBuffer) & 0xFF);
            }
        }

        return output;
    }
    public static int unsignedByteToInt(byte b) {
        return b & 255;
    }
}
