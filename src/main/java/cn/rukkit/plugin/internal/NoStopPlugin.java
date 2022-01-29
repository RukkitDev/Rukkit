package cn.rukkit.plugin.internal;

import cn.rukkit.plugin.PluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoStopPlugin extends InternalRukkitPlugin{

    Logger log = LoggerFactory.getLogger(NoStopPlugin.class);

    @Override
    public void onLoad() {
        log.info("NoStop::onLoad");
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
        config.apiVersion = "ANY";
        config.author = "Rukkit";
        config.version = "1.0.0";
        config.pluginClass = "cn.rukkit.plugin.internal.NoStopPlugin";
        config.name = "NoStopPlugin";
    }
}
