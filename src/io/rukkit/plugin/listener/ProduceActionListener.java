package io.rukkit.plugin.listener;

import io.rukkit.entity.*;
import io.rukkit.game.*;

@Deprecated
public interface ProduceActionListener extends ActionListener
{
	public boolean onProduce(Player p,ProduceAction action);
}
