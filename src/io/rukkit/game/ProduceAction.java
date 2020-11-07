package io.rukkit.game;
import io.rukkit.command.*;

public class ProduceAction extends GameAction
{
	public String targetUnit;
	public long produceFromUnitId;
	
	/*
	@Override
	public GameActions type = GameActions.PRODUCE;*/
	
	public ProduceAction(){
		type = GameActions.PRODUCE;
	}
}
