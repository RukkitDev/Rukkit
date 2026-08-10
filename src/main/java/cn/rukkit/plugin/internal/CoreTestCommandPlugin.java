/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.plugin.internal;

import cn.rukkit.Rukkit;
import cn.rukkit.command.ChatCommand;
import cn.rukkit.command.ChatCommandContext;
import cn.rukkit.command.ChatCommandContextListener;
import cn.rukkit.command.CommandManager;
import cn.rukkit.command.RoomCommandContext;
import cn.rukkit.event.EventHandler;
import cn.rukkit.event.EventListener;
import cn.rukkit.event.action.PingEvent;
import cn.rukkit.game.NetworkPlayer;
import cn.rukkit.plugin.PluginConfig;

import java.io.File;
import java.io.IOException;

/** Core-network counterpart of the legacy test/debug command plugin. */
public class CoreTestCommandPlugin extends InternalRukkitPlugin implements EventListener {

    private TestPluginConfig testConfig = new TestPluginConfig();

    class TestSyncCallback implements ChatCommandContextListener {
        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            if (context instanceof RoomCommandContext room) {
                room.room().syncGame();
            }
            return false;
        }
    }

    class SummonCallback implements ChatCommandContextListener {
        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            if (!(context instanceof RoomCommandContext room)) {
                return false;
            }
            if (!room.room().isGaming()) {
                context.sendServerMessage("游戏未开始！");
                return false;
            }
            if (args.length >= 1) {
                long previous = (long) context.player().getTempData("lastSummonTime", 0L);
                long now = System.currentTimeMillis();
                if (now - previous < testConfig.cd) {
                    context.sendServerMessage("请等待" + ((now - previous) / 1000) + "秒");
                    return false;
                }
                context.sendServerMessage("请PING一个位置");
                context.player().putTempData("spawnUnit", args[0]);
                context.player().putTempData("isSpawnTriggered", true);
                context.player().putTempData("lastSummonTime", now);
            }
            return false;
        }
    }

    @EventHandler
    public void onPing(PingEvent event) {
        NetworkPlayer player = event.getPlayer();
        if (!(player.getServerRoom() != null)
                || !(boolean) player.getTempData("isSpawnTriggered", false)) {
            return;
        }
        String unit = (String) player.getTempData("spawnUnit", "tank");
        if (unit.equals("editorOrBuilder") && !player.isAdmin
                && player.getServerConnection() != null) {
            player.getServerConnection().sendServerMessage("只有管理才可以生成该单位！");
        }
        player.getServerRoom().summonUnit(unit, event.getTargetX(), event.getTargetY(),
                player.playerIndex);
        player.putTempData("isSpawnTriggered", false);
    }

    class StopCallback implements ChatCommandContextListener {
        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            if (context instanceof RoomCommandContext room) {
                room.room().stopGame(true);
            }
            return false;
        }
    }

    @Override
    public void onLoad() {
        getLogger().info("CoreTestCommandPlugin is loading...");
        testConfig = new TestPluginConfig();
        getPluginManager().registerEventListener(this, this);
        try {
            File pluginFile = getConfigFile("config");
            if (pluginFile.length() == 0) {
                saveConfig(pluginFile, testConfig);
            }
            testConfig = getConfig(pluginFile, TestPluginConfig.class);
        } catch (IOException e) {
            getLogger().warn("Config cannot be loaded.");
        }

        CommandManager manager = Rukkit.getCommandManager();
        manager.registerCommand(ChatCommand.contextCommand("summon", "Summon a unit.", 1,
                new SummonCallback(), this));
        manager.registerCommand(ChatCommand.contextCommand("gamestop",
                "Stop a game immidately and return to the battleroom", 0,
                new StopCallback(), this));
        manager.registerCommand(ChatCommand.contextCommand("testsync", "Sync", 0,
                new TestSyncCallback(), this));
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onStart() {
        getLogger().info("Core test plugin is starting..");
    }

    @Override
    public void onDone() {
    }

    @Override
    public void loadConfig() {
        config = new PluginConfig();
        config.name = "CoreTestPlugin";
        config.author = "rukkit";
        config.version = "1.0.0";
        config.id = "core-test-plugin";
        config.pluginClass = "cn.rukkit.plugin.internal.CoreTestCommandPlugin";
        config.apiVersion = Rukkit.PLUGIN_API_VERSION;
    }
}
