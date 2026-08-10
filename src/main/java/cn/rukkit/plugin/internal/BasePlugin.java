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
import cn.rukkit.game.NetworkPlayer;
import cn.rukkit.network.NetworkRoom;
import cn.rukkit.plugin.PluginConfig;
import cn.rukkit.util.LangUtil;
import cn.rukkit.util.VersionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;

public class BasePlugin extends InternalRukkitPlugin implements EventListener {

    private Logger log = LoggerFactory.getLogger("Rukkit");


    @EventHandler
    public void onPlayerJoinTip(PlayerJoinEvent event) {
        NetworkPlayer player = event.getPlayer();
        broadcastServerMessage(player,
                MessageFormat.format(LangUtil.getString("rukkit.playerJoin"), player.name));
        LoggerFactory.getLogger("Room #" + roomId(player)).info("Player {} joined!", player.name);
    }

    @EventHandler
    public void onPlayerLeaveTip(PlayerLeftEvent event) {
        NetworkPlayer player = event.getPlayer();
        broadcastServerMessage(player,
                MessageFormat.format(LangUtil.getString("rukkit.playerLeft"),
                        player.name, event.getReason()));
        if (isGaming(player)) {
            player.sendTeamMessage(LangUtil.getString("rukkit.playerSharingControlDueDisconnected"));
        }
        LoggerFactory.getLogger("Room #" + roomId(player)).info("Player {} left!({})",
                player.name, event.getReason());
        player.savePlayerData();
    }

    @EventHandler
    public void onPlayerChatInfo(PlayerChatEvent event) {
        LoggerFactory.getLogger("Room #" + roomId(event.getPlayer())).info("[{}] {}",
                event.getPlayer().name, event.getMessage());
    }

    @EventHandler
    public void onPlayerReconnected(PlayerReconnectEvent event) {
        NetworkPlayer player = event.getPlayer();
        broadcastServerMessage(player,
                MessageFormat.format(LangUtil.getString("rukkit.playerReconnect"), player.name));
        LoggerFactory.getLogger("Room #" + roomId(player)).info("Player {} reconnected!", player.name);
    }

    private static void broadcastServerMessage(NetworkPlayer player, String message) {
        if (player.getServerRoom() != null) {
            player.getServerRoom().connectionManager.broadcastServerMessage(message);
        } else if (player.getRoom() != null) {
            player.getRoom().connectionManager.broadcastServerMessage(message);
        }
    }

    private static boolean isGaming(NetworkPlayer player) {
        if (player.getServerRoom() != null) {
            return player.getServerRoom().isGaming();
        }
        return player.getRoom() != null && player.getRoom().isGaming();
    }

    private static int roomId(NetworkPlayer player) {
        if (player.getServerRoom() != null) {
            return player.getServerRoom().roomId;
        }
        NetworkRoom room = player.getRoom();
        return room == null ? -1 : room.roomId;
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
        config.version = VersionUtil.getVersion();
        config.id = "base-plugin";
        config.pluginClass = "cn.rukkit.plugin.internal.BasePlugin";
        config.apiVersion = Rukkit.PLUGIN_API_VERSION;
    }
}
