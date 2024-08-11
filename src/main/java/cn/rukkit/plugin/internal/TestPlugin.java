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
import cn.rukkit.command.ChatCommandListener;
import cn.rukkit.event.EventHandler;
import cn.rukkit.event.EventListener;
import cn.rukkit.event.action.PingEvent;
import cn.rukkit.game.NetworkPlayer;
import cn.rukkit.network.RoomConnection;
import cn.rukkit.network.packet.Packet;
import cn.rukkit.plugin.PluginConfig;

import java.io.File;
import java.io.IOException;

public class TestPlugin extends InternalRukkitPlugin implements EventListener {

    TestPluginConfig testConfig = new TestPluginConfig();

    class TestSyncCallback implements ChatCommandListener {
        @Override
        public boolean onSend(RoomConnection con, String[] args) {
            con.currectRoom.syncGame();
            return false;
        }
    }

    class SummonCallback implements ChatCommandListener {
        @Override
        public boolean onSend(RoomConnection con, String[] args) {
            if (!con.currectRoom.isGaming()) {
                con.sendServerMessage("游戏未开始！");
                return false;
            }
            if (args.length >= 1) {
                long time = (long) con.player.getTempData("lastSummonTime", 0L);
                long currentTime = System.currentTimeMillis();
                if (currentTime - time < testConfig.cd) {
                    con.player.getConnection().sendServerMessage("请等待" + ((currentTime - time) / 1000)  + "秒");
                    return false;
                }
                con.sendServerMessage("请PING一个位置");
                con.player.putTempData("spawnUnit", args[0]);
                con.player.putTempData("isSpawnTriggered", true);
                con.player.putTempData("lastSummonTime", System.currentTimeMillis());
            }
            return false;
        }
    }

    @EventHandler
    public void onPing(PingEvent event) {
        NetworkPlayer player = event.getPlayer();
        boolean isTriggered = (boolean) player.getTempData("isSpawnTriggered", false);
        String unit = (String) event.getPlayer().getTempData("spawnUnit", "tank");
        if (isTriggered) {
            if (unit.equals("editorOrBuilder")) {
                if (!player.isAdmin) {
                    player.getConnection().sendServerMessage("只有管理才可以生成该单位！");
                }
            }
            try {
                player.getRoom().broadcast(Packet.gameSummon(player.getRoom(), unit, event.getTargetX(), event.getTargetY(), player.playerIndex));
            } catch (IOException ignored) {}
            player.putTempData("isSpawnTriggered", false);
        }
    }

    public class StopCallback implements ChatCommandListener {
        @Override
        public boolean onSend(RoomConnection con, String[] args) {
            con.currectRoom.stopGame(true);
            return false;
        }
    }

    @Override
    public void onLoad() {
        getLogger().info("TestPlugin is loading...");
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
        Rukkit.getCommandManager().registerCommand(new ChatCommand("summon", "Summon a unit.", 1, new SummonCallback(), this));
        Rukkit.getCommandManager().registerCommand(new ChatCommand("gamestop", "Stop a game immidately and return to the battleroom", 0, new StopCallback(), this));
        Rukkit.getCommandManager().registerCommand(new ChatCommand("testsync", "Sync", 0, new TestSyncCallback(), this));
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }

    @Override
    public void onStart() {
        getLogger().info("Plugin is starting..");
    }

    @Override
    public void onDone() {

    }

    @Override
    public void loadConfig() {
        config = new PluginConfig();
        config.name = "TestPlugin2.0";
        config.author = "rukkit";
        config.version = "1.0.0";
        config.id = "test-plugin";
        config.pluginClass = "cn.rukkit.plugin.internal.NewTestPlugin";
        config.apiVersion = Rukkit.PLUGIN_API_VERSION;
    }
}
