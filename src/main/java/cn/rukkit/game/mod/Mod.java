/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.game.mod;

import java.util.*;

public class Mod 
{
	public static class ModUnit {
		private String modName;
		private String unitName;
		private int unitId;

		public ModUnit() {
			super();
		}

		public ModUnit(String unit, int id, String modname) {
			this.modName = modname;
			this.unitId = id;
			this.unitName = unit;
		}

		public void setModName(String modName)
		{
			this.modName = modName;
		}

		public String getModName()
		{
			return modName;
		}

		public void setUnitName(String unitName)
		{
			this.unitName = unitName;
		}

		public String getUnitName()
		{
			return unitName;
		}

		public void setUnitId(int unitId)
		{
			this.unitId = unitId;
		}

		public int getUnitId()
		{
			return unitId;
		}
	}
	
	private boolean isEnabled = true;

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
