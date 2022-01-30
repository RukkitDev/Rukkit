/*
 *  All Rights Reserved.
 *  FileName: NoStopPlugin.java
 *  @author: wtbdev
 *  @date: 2022/1/30 下午4:37
 */

package cn.rukkit.plugin.internal;

import cn.rukkit.Rukkit;
import cn.rukkit.event.EventHandler;
import cn.rukkit.event.EventListener;
import cn.rukkit.event.player.PlayerJoinEvent;
import cn.rukkit.game.NetworkPlayer;
import cn.rukkit.network.Connection;
import cn.rukkit.plugin.PluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class NoStopPlugin extends InternalRukkitPlugin implements EventListener {

    Logger log = LoggerFactory.getLogger(NoStopPlugin.class);
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {

    }

    @Override
    public void onLoad() {
        log.info("NoStop::onLoad");
        log.info("Preparing PlayerList...");
        NetworkPlayer emptyPlayer = new NetworkPlayer(null);
        emptyPlayer.name = "Empty";
        Arrays.fill(Rukkit.getConnectionManager().getPlayerManager().getPlayerArray(), emptyPlayer);

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
