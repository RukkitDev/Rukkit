package io.rukkit.mod;

import io.rukkit.entity.*;
import java.util.*;

public class Mod
{
	private boolean isEnabled;

	private ArrayList<ModUnit> unitList;
	
	public Mod(ArrayList<ModUnit> list) {
		this.unitList = list;
	}
	
	public ArrayList<ModUnit> getUnitList() {
		return unitList;
	};
	
	public void setEnabled(boolean bool) {
		this.isEnabled = bool;
	}
	
	public boolean isEnabled() {
		return isEnabled;
	}
}
