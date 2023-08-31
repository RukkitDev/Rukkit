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
import cn.rukkit.command.CommandManager;
import cn.rukkit.event.EventHandler;
import cn.rukkit.event.EventListener;
import cn.rukkit.event.action.BuildEvent;
import cn.rukkit.event.action.MoveEvent;
import cn.rukkit.event.action.PingEvent;
import cn.rukkit.event.action.TaskEvent;
import cn.rukkit.event.player.PlayerChatEvent;
import cn.rukkit.event.room.RoomStartGameEvent;
import cn.rukkit.event.server.ServerQuestionRespondEvent;
import cn.rukkit.network.RoomConnection;
import cn.rukkit.network.packet.Packet;
import cn.rukkit.plugin.PluginConfig;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

public class TestPlugin extends InternalRukkitPlugin implements EventListener {
	String[] team;
	long[] cd;
	class SummonCallback implements ChatCommandListener {
		@Override
		public boolean onSend(RoomConnection con, String[] args) {
			if (!con.currectRoom.isGaming()) {
				con.sendServerMessage("游戏未开始！");
				return false;
			}
			if (args.length >= 1) {
				String name = args[0];
				team[con.player.playerIndex] = name;
				con.sendServerMessage("请PING一个位置");
			}
			return false;
		}
	}

	class DumpSaveCallback implements ChatCommandListener {
		@Override
		public boolean onSend(RoomConnection con, String[] args) {
			if (con.save != null) {
				File f = new File(Rukkit.getEnvPath() + con.player.name + "-" + Rukkit.getRoundConfig().mapName + "-" + con.currectRoom.getCurrentStep() + ".rwsave");
				try {
					con.save.dumpToFile(f);
					con.sendServerMessage("Succeed to dump save.");
				} catch (IOException e) {
					con.sendServerMessage("Failed to dump save.");
				}
			} else {
				con.sendServerMessage("Save is not availavle!");
			}
			return false;
		}
	}

	class DZCallback implements ChatCommandListener {
		@Override
		public boolean onSend(RoomConnection con, String[] args) {
			try {
				con.handler.ctx.writeAndFlush(Packet.packetQuestion(1376, "你是哪个省的"));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return false;
		}
	}

	@EventHandler
	public void onQuestionResponse(ServerQuestionRespondEvent event) {
		if (event.getQid() == 1376) {
			event.getPlayer().getConnection().sendChat("我是妈妈生的");
		}
	}

	@EventHandler
	public void onChat(PlayerChatEvent event) {
		//event.getPlayer().getConnection().sendChat("[测试TAG] " + event.getMessage());
		//event.setCancel(true);
	}

	@EventHandler
	public void onBuild(BuildEvent e) {
//		e.getPlayer().getConnection()
//			.sendServerMessage(String.format("%s, 你在(%f,%f) 造了单位 %s, ID为%d", e.getPlayer().name, e.getTargetX(), e.getTargetY(), e.getTargetUnitName(), e.getFromUnitId()));
//		if (e.getTargetUnitName().equals("c_turret_t1")) {
//			try {
//				for (int i = 0;i < 10;i++) {
//					e.getPlayer().getRoom().broadcast(Packet.gameSummon(
//							e.getPlayer().getRoom(),
//							"c_turret_t2_artillery",
//							e.getTargetX(),
//							e.getTargetY(),
//							e.getPlayer().playerIndex
//					));
//				}
//			} catch (IOException ignored) {}
//		} else if (e.getTargetUnitName().equals("c_antiAirTurret")) {
//			try {
//				for (int i = 0;i < 10;i++) {
//					e.getPlayer().getRoom().broadcast(Packet.gameSummon(
//							e.getPlayer().getRoom(),
//							"c_antiAirTurretT3",
//							e.getTargetX(),
//							e.getTargetY(),
//							e.getPlayer().playerIndex
//					));
//				}
//			} catch (IOException ignored) {
//			}
//		}
	}

	@EventHandler
	public void onTask(TaskEvent e) {
		//e.getPlayer().getConnection()
		//	.sendServerMessage(String.format("%s, 你在ID为%d的单位上执行了%s,是否为取消=%b", e.getPlayer().name, e.getActionUnitId(), e.getTaskString(), e.isCancel()));
	}

	@EventHandler
	public void onMove(MoveEvent e) {
		//e.getPlayer().getConnection()
		//	.sendServerMessage(String.format("%s, 你移动了ID为%d的单位,目的地为(%f,%f)", e.getPlayer().name, e.getActionUnitId(), e.getTargetX(), e.getTargetY()));
	}

//	@EventHandler
//	public void onGameStart(RoomStartGameEvent e) {
//		try {
//			e.getRoom().connectionManager.broadcast(
//					Packet.packetQuestion(114514,
//							"- 熬鹰模式 - \n" +
//									"- 建造陆地炮塔时自动生成15个T2火炮\n" +
//									"- 建造空军炮塔时自动生成15个T3防空\n" +
//									"- 玩的愉快 :) 点确定即可进入游戏！"));
//		} catch (IOException ex) {}
//	}

	@EventHandler
	public void onPing(PingEvent e) {
		//e.getPlayer().getConnection()
		//	.sendServerMessage(String.format("%s, 你发出了PING:%s,PING目标为(%f,%f)", e.getPlayer().name, e.getPingType(), e.getTargetX(), e.getTargetY()));
		if (!team[e.getPlayer().playerIndex].equals("nop")) {
			long time = System.currentTimeMillis();
			if ((time - cd[e.getPlayer().playerIndex]) < testConfig.cd) {
				e.getPlayer().getConnection().sendServerMessage("尚未冷却，剩余" +
						((time - cd[e.getPlayer().playerIndex]) / 1000.0f) + "秒！");
				e.setCancel(true);
				return;
			}
			try {
				e.getPlayer().getRoom().broadcast(Packet.gameSummon(e.getPlayer().getRoom(), team[e.getPlayer().playerIndex], e.getTargetX(), e.getTargetY(), e.getPlayer().playerIndex));
				e.getPlayer().getConnection().sendServerMessage("生成成功！");
			} catch (IOException err) {
				e.getPlayer().getConnection().sendServerMessage("生成失败！");
			} finally {
				team[e.getPlayer().playerIndex] = "nop";
				cd[e.getPlayer().playerIndex] = time;
				e.setCancel(true);
			}
		}
	}

	TestPluginConfig testConfig = new TestPluginConfig();
	@Override
	public void onLoad() {
		// TODO: Implement this method
		getLogger().info("TestPlugin is loading...");
		getPluginManager().registerEventListener(this, this);
		CommandManager mgr = Rukkit.getCommandManager();
		try {
			File pluginFile = getConfigFile("config");
			if (pluginFile.length() == 0) {
				saveConfig(pluginFile, testConfig);
			}
			testConfig = getConfig(pluginFile, TestPluginConfig.class);
		} catch (IOException e) {
			getLogger().warn("Config cannot be loaded.");
		}
		team = new String[Rukkit.getConfig().maxPlayer];
		cd = new long[Rukkit.getConfig().maxPlayer];
		Arrays.fill(team, "nop");
		Arrays.fill(cd, 0L);
		mgr.registerCommand(new ChatCommand("summon", "Summon a unit.", 1, new SummonCallback(), this));
		mgr.registerCommand(new ChatCommand("dumpsave", "Dump a save to server", 0, new DumpSaveCallback(), this));
		mgr.registerCommand(new ChatCommand("mother", "大脑升级", 0, new DZCallback(), this));
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
