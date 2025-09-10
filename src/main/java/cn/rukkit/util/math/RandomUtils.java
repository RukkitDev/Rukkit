/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *  
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package cn.rukkit.util.math;

import java.util.Random;

/**
 * @author Dr (dr@der.kim)
 */
public final class RandomUtils {
    private static final CharRange[] defaultRanges = new CharRange[]{
        new CharRange('a', 'z'),
        new CharRange('A', 'Z'),
        new CharRange('0', '9')
    };
    
    private static final CharRange[] letterRanges = new CharRange[]{
        new CharRange('a', 'z'),
        new CharRange('A', 'Z')
    };
    
    private static final CharRange[] intCharRanges = new CharRange[]{
        new CharRange('0', '9')
    };

    private RandomUtils() {} // Prevent instantiation

    public static byte[] getRandomByteArray(int length) {
        return getRandomByteArray(length, new Random());
    }

    public static byte[] getRandomByteArray(int length, Random random) {
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = (byte) random.nextInt(256);
        }
        return result;
    }

    /**
     * 随机生成一个正整数
     */
    public static int getRandomUnsignedInt() {
        return Math.abs(new Random().nextInt());
    }

    /**
     * 随机生成长度为 length 的 String.
     */
    public static String getRandomString(int length) {
        return getRandomString(length, new Random());
    }

    public static String getRandomString(int length, Random random) {
        return getRandomString(length, defaultRanges, random);
    }

    /**
     * 根据所给 charRange 随机生成长度为 length 的 String.
     */
    public static String getRandomString(int length, CharRange charRange) {
        return getRandomString(length, charRange, new Random());
    }

    public static String getRandomString(int length, CharRange charRange, Random random) {
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = charRange.random(random);
        }
        return new String(chars);
    }

    /**
     * 根据所给 charRanges 随机生成长度为 length 的 String.
     */
    public static String getRandomString(int length, CharRange[] charRanges) {
        return getRandomString(length, charRanges, new Random());
    }

    public static String getRandomString(int length, CharRange[] charRanges, Random random) {
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            CharRange range = charRanges[random.nextInt(charRanges.length)];
            chars[i] = range.random(random);
        }
        return new String(chars);
    }

    /**
     * 产生len长度的随机数字
     * @param length
     * @return
     */
    public static String getRandomIntString(int length) {
        return getRandomIntString(length, new Random());
    }

    public static String getRandomIntString(int length, Random random) {
        return getRandomString(length, intCharRanges, random);
    }

    /**
     * 产生len长度的随机字母串(只包含大小写字母)
     * @param length 随机字符串长度
     * @return
     */
    public static String getRandomIetterString(int length) {
        return getRandomIetterString(length, new Random());
    }

    public static String getRandomIetterString(int length, Random random) {
        return getRandomString(length, letterRanges, random);
    }

    /**
     * 返回一个定长的随机纯大写字母字符串(只包含大写字母)
     * @param length 随机字符串长度
     * @return 随机字符串
     */
    public static String generateLowerStr(int length) {
        return getRandomIetterString(length).toLowerCase();
    }

    /**
     * 返回一个定长的随机纯小写字母字符串(只包含小写字母)
     * @param length 随机字符串长度
     * @return 随机字符串
     */
    public static String generateUpperStr(int length) {
        return getRandomIetterString(length).toUpperCase();
    }

    // Helper class to represent character ranges
    private static class CharRange {
        private final char start;
        private final char end;

        public CharRange(char start, char end) {
            this.start = start;
            this.end = end;
        }

        public char random(Random random) {
            return (char) (random.nextInt(end - start + 1) + start);
        }
    }
}