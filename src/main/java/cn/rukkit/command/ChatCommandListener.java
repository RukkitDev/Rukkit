/*
 *  All Rights Reserved.
 *  FileName: ChatCommandListener.java
 *  @author: wtbdev
 *  @date: 2022/1/30 下午4:37
 */

package cn.rukkit.command;
import cn.rukkit.network.*;

public interface ChatCommandListener
{
	public boolean onSend(Connection con, String args[]);
}
