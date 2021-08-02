package cn.rukkit.plugin.internal;

import cn.rukkit.plugin.*;
import cn.rukkit.event.player.*;
import cn.rukkit.event.*;
import cn.rukkit.*;
import cn.rukkit.network.packet.*;
import cn.rukkit.event.action.*;

public class TestPlugin extends InternalRukkitPlugin implements EventListener
{
	@EventHandler
	public void onChat(PlayerChatEvent event) {
		event.getPlayer().getConnection().sendChat("[测试TAG] " + event.getMessage());
		event.setCancel(true);
	}
	
	@EventHandler
	public void onBuild(BuildEvent e) {
		e.getPlayer().getConnection()
			.sendServerMessage(String.format("%s, 你在(%f,%f) 造了单位 %s, ID为%d",e.getPlayer().name, e.getTargetX(), e.getTargetY(), e.getTargetUnitName(), e.getFromUnitId()));
	}
	
	@EventHandler
	public void onTask(TaskEvent e) {
		e.getPlayer().getConnection()
			.sendServerMessage(String.format("%s, 你在ID为%d的单位上执行了%s,是否为取消=%b",e.getPlayer().name, e.getActionUnitId(), e.getTaskString(), e.isCancel()));
	}
	
	@EventHandler
	public void onMove(MoveEvent e) {
		e.getPlayer().getConnection()
			.sendServerMessage(String.format("%s, 你移动了ID为%d的单位,目的地为(%f,%f)",e.getPlayer().name, e.getActionUnitId(), e.getTargetX(), e.getTargetY()));
	}
	
	@EventHandler
	public void onPing(PingEvent e) {
		e.getPlayer().getConnection()
			.sendServerMessage(String.format("%s, 你发出了PING:%s,PING目标为(%f,%f)",e.getPlayer().name, e.getPingType(), e.getTargetX(), e.getTargetY()));
	}
	
	@Override
	public void onLoad() {
		// TODO: Implement this method
		getLogger().info("TestPlugin is loading...");
		getPluginManager().registerEventListener(this, this);
	}

	@Override
	public void onEnable() {
		// TODO: Implement this method
	}

	@Override
	public void onDisable() {
		// TODO: Implement this method
	}

	@Override
	public void onStart() {
		// TODO: Implement this method
	}

	@Override
	public void onDone() {
		// TODO: Implement this method
	}

	@Override
	public void loadConfig() {
		config = new PluginConfig();
		config.name = "公屏聊天";
		config.author = "rukkit";
		config.version = "1.0.0";
		config.pluginClass = "cn.rukkit.plugin.internal.TestPlugin";
		config.apiVersion = "ANY";
		// TODO: Implement this method
	}
	
}
