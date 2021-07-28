package cn.rukkit.command;
import cn.rukkit.network.*;

public interface ChatCommandListener
{
	public boolean onSend(Connection con, String args[]);
}
