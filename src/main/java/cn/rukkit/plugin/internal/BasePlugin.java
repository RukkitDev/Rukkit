/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 */

package cn.rukkit.plugin.internal;

import cn.rukkit.Rukkit;
import cn.rukkit.event.EventHandler;
import cn.rukkit.event.EventListener;
import cn.rukkit.event.player.PlayerChatEvent;
import cn.rukkit.event.player.PlayerJoinEvent;
import cn.rukkit.event.player.PlayerLeftEvent;
import cn.rukkit.event.player.PlayerReconnectEvent;
import cn.rukkit.network.RoomConnection;
import cn.rukkit.plugin.PluginConfig;
import cn.rukkit.util.LangUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;

public class BasePlugin extends InternalRukkitPlugin implements EventListener {

    private Logger log = LoggerFactory.getLogger("Rukkit");


    @EventHandler
    public void onPlayerJoinTip(PlayerJoinEvent event) {
        event.getPlayer().getRoom().connectionManager.broadcastServerMessage(MessageFormat.format(LangUtil.getString("rukkit.playerJoin"), event.getPlayer().name));
        LoggerFactory.getLogger("Room #" + event.getPlayer().getRoom().roomId).info("Player {} joined!", event.getPlayer().name);
    }

    @EventHandler
    public void onPlayerLeaveTip(PlayerLeftEvent event) {
        event.getPlayer().getRoom().connectionManager.broadcastServerMessage(MessageFormat.format(LangUtil.getString("rukkit.playerLeft"), event.getPlayer().name, event.getReason()));
        if (event.getPlayer().getRoom().isGaming()) {
            event.getPlayer().sendTeamMessage(LangUtil.getString("rukkit.playerSharingControlDueDisconnected"));
        }
        LoggerFactory.getLogger("Room #" + event.getPlayer().getRoom().roomId).info("Player {} left!({})", event.getPlayer().name, event.getReason());
        event.getPlayer().savePlayerData();
    }

    @EventHandler
    public void onPlayerChatInfo(PlayerChatEvent event) {
        LoggerFactory.getLogger("Room #" + event.getPlayer().getRoom().roomId).info("[{}] {}", event.getPlayer().name, event.getMessage());
    }

    @EventHandler
    public void onPlayerReconnected(PlayerReconnectEvent event) {
        event.getPlayer().getRoom().connectionManager.broadcastServerMessage(MessageFormat.format(LangUtil.getString("rukkit.playerReconnect"), event.getPlayer().name));
        LoggerFactory.getLogger("Room #" + event.getPlayer().getRoom().roomId).info("Player {} reconnected!", event.getPlayer().name);
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
        getLogger().info("PlayerManager::Saving Player Data...");
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
        config.name = "Basic Game Plugin";
        config.author = "rukkit";
        config.version = Rukkit.RUKKIT_VERSION;
        config.id = "base-plugin";
        config.pluginClass = "cn.rukkit.plugin.internal.BasePlugin";
        config.apiVersion = Rukkit.PLUGIN_API_VERSION;
    }
}
