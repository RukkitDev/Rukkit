package io.rukkit.game;

import io.rukkit.command.*;

public class BuildAction extends GameAction
{
	public String targetUnit;
	public long buildFrom;
	public BuildAction(){
		type = GameActions.BUILD;
	}
}
