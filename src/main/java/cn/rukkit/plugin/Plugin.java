package cn.rukkit.plugin;

public interface Plugin
{
	public void onLoad();
	public void onEnable();
	public void onDisable();
	public void onStart();
	public void onDone();
}
