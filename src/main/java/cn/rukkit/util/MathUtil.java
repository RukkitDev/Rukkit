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
}
