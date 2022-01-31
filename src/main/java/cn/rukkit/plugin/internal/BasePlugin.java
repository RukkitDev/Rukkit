package cn.rukkit.plugin.internal;

import cn.rukkit.Rukkit;
import cn.rukkit.event.EventHandler;
import cn.rukkit.event.EventListener;
import cn.rukkit.event.player.PlayerJoinEvent;
import cn.rukkit.event.player.PlayerLeftEvent;
import cn.rukkit.plugin.PluginConfig;

public class BasePlugin extends InternalRukkitPlugin implements EventListener {

    @EventHandler
    public void onPlayerJoinTip(PlayerJoinEvent event) {
        Rukkit.getConnectionManager().broadcastServerMessage(event.getPlayer().name + " joined the server!");
    }

    @EventHandler
    public void onPlayerLeaveTip(PlayerLeftEvent event) {
        Rukkit.getConnectionManager().broadcastServerMessage(event.getPlayer().name + " left the server!");
    }

    @Override
    public void onLoad() {
        getLogger().info("BasePlugin::Load");
        getPluginManager().registerEventListener(this, this);
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }

    @Override
    public void onStart() {

    }

    @Override
    public void onDone() {

    }

    @Override
    public void loadConfig() {
        config = new PluginConfig();
        config.name = "base-plugin";
        config.author = "rukkit";
        config.version = "1.0.0";
        config.pluginClass = "cn.rukkit.plugin.internal.BasePlugin";
        config.apiVersion = "ANY";
    }
}
