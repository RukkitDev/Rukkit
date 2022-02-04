/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.util;


public class GameUtils
{
	public static int getMoneyFormat(int n2)
	{
        if (n2 == 4000)
		{
            return 0;
        }
        if (n2 == 0)
		{
            return 1;
        }
        if (n2 == 1000)
		{
            return 2;
        }
        if (n2 == 2000)
		{
            return 3;
        }
        if (n2 == 5000)
		{
            return 4;
        }
        if (n2 == 10000)
		{
            return 5;
        }
        if (n2 == 50000)
		{
            return 6;
        }
        if (n2 == 100000)
		{
            return 7;
        }
        if (n2 != 200000) return 8;
        return 8;
    }
}

