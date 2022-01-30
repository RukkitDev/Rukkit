/*
 *  All Rights Reserved.
 *  FileName: GameUtils.java
 *  @author: wtbdev
 *  @date: 2022/1/30 下午4:37
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

