package io.rukkit.plugin.listener;

import io.rukkit.entity.*;
import io.rukkit.game.*;

@Deprecated
public interface BuildActionListener extends ActionListener
{
	public boolean onBuild(Player p,BuildAction action);
}
