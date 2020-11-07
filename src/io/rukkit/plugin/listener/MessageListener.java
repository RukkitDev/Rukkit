package io.rukkit.plugin.listener;
import io.rukkit.entity.*;

@Deprecated
public interface MessageListener extends BaseListener
{
	void onRecieve(Player p, String msg);
}
