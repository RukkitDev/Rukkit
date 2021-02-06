package io.rukkit.entity;

public class ModUnit
{
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
