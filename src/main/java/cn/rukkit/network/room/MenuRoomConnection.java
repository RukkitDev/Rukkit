/*
 * Copyright 2025 Micro(MCLDY@outlook.com) and contributors.
 * 
 * 本衍生作品基于 AGPLv3 许可证
 * This derivative work is licensed under AGPLv3
 */
package cn.rukkit.network.room;

import cn.rukkit.Rukkit;
import cn.rukkit.game.NetworkPlayer;
import cn.rukkit.network.core.ConnectionHandler;
import cn.rukkit.network.core.packet.Packet;
import cn.rukkit.network.core.packet.PacketType;
import cn.rukkit.network.core.packet.UniversalPacket;
import cn.rukkit.network.io.GameOutputStream;
import cn.rukkit.network.io.GzipEncoder;
import cn.rukkit.util.GameUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import java.util.concurrent.ScheduledFuture;

import org.jline.utils.Log;

public class MenuRoomConnection {
	public NetworkPlayer player;
	public ConnectionHandler handler;
	public ServerRoom currectRoom;
	public long pingTime;
	private static final int MAX_MENU_SIZE = 10;

	// 菜单系统相关变量
	private Stack<List<MenuOption>> menuStack = new Stack<>(); // 菜单栈，用于支持多级菜单
	private List<MenuOption> currentMenu = new ArrayList<MenuOption>(); // 当前显示的菜单

	// 根菜单（主菜单）
	private List<MenuOption> rootMenu = new ArrayList<MenuOption>();

	private ScheduledFuture pingFuture;
	private ScheduledFuture teamFuture;

	public MenuRoomConnection(ConnectionHandler handler, ServerRoom currectRoom) {
		this.handler = handler;
		this.currectRoom = currectRoom;

		// 初始化菜单系统
		// 创建根菜单
		createRootMenu();

		// 设置当前菜单为根菜单
		currentMenu = rootMenu;
		menuStack.clear();
	}

	/**
	 * 创建根菜单
	 */
	private void createRootMenu() {
		rootMenu.clear();

		MenuOption keep = createTextOption(0, "[文本]占位");
		rootMenu.add(keep);

		// 添加教程菜单项
		rootMenu.add(createMenuOption(1, "[菜单]我是教程：请尝试踢出我", new MenuOption.Callback() {
			@Override
			public void onClick() {
				// 进入教程子菜单
				navigateToMenu(createTutorialMenu());
			}
		}));
		rootMenu.add(createMenuOption(2, "[按钮]查看个人信息", new MenuOption.Callback() {
			@Override
			public void onClick() {
				sendServerMessage("未完成");
			}
		}));

		// 这里可以添加更多根菜单项
		// 例如:
		// MenuOption settingsOption = createMenuOption(1, "[菜单] 设置", () ->
		// navigateToMenu(createSettingsMenu()));
		// rootMenu.add(settingsOption);
	}

	/**
	 * 创建教程子菜单
	 */
	private List<MenuOption> createTutorialMenu() {
		List<MenuOption> tutorialMenu = new ArrayList<>();

		MenuOption keep = createTextOption(0, "[文本]占位");
		tutorialMenu.add(keep);

		// 返回按钮
		MenuOption backOption = new MenuOption();
		backOption.optionId = (byte) 1;
		backOption.index = backOption.optionId;
		backOption.name = "[按钮]返回";
		backOption.team = 0;
		backOption.ping = 1;
		backOption.callback = this::goBack;
		tutorialMenu.add(backOption);

		// 教程文本1

		MenuOption text1 = createTextOption(2, "[文本]棒棒的!我们把踢出定义为\"按下\"");
		tutorialMenu.add(text1);

		MenuOption text0 = createTextOption(3, "[文本]有时文字不全,点开我看看[---------------------hi你看到我了吗]");
		tutorialMenu.add(text0);
		// 教程文本2
		MenuOption text2 = createTextOption(4, "[文本]在列表上的选项类型有\"选项、文本.\"");
		tutorialMenu.add(text2);

		// 教程文本3
		MenuOption text3 = createTextOption(5, "[文本]看完了吗？尝试\"按下\":[按钮]返回");
		tutorialMenu.add(text3);

		return tutorialMenu;
	}

	// ///////////// //
	// MENU CORE API //
	// ///////////// //
	/**
	 * 创建文本选项（不可点击）
	 */
	private MenuOption createTextOption(int id, String text) {
		MenuOption option = new MenuOption();
		option.optionId = (byte) id;
		option.index = option.optionId;
		option.name = text;
		option.team = 0;
		option.ping = 1;
		option.callback = () -> {
		}; // 空回调，表示不可点击
		return option;
	}

	/**
	 * 创建菜单选项（可点击）
	 */
	private MenuOption createMenuOption(int id, String text, MenuOption.Callback callback) {
		MenuOption option = new MenuOption();
		option.optionId = (byte) id;
		option.index = option.optionId;
		option.name = text;
		option.team = 0;
		option.ping = 1;
		option.callback = callback;
		return option;
	}

	/**
	 * 导航到指定菜单
	 */
	private void navigateToMenu(List<MenuOption> newMenu) {
		// 将当前菜单压入栈中
		menuStack.push(currentMenu);
		// 设置新菜单为当前菜单
		currentMenu = newMenu;
		// 更新菜单显示
		updateMenuDisplay();
	}

	/**
	 * 返回上一级菜单
	 */
	private void goBack() {
		if (!menuStack.isEmpty()) {
			currentMenu = menuStack.pop();
			updateMenuDisplay();
		} else {
			// 如果栈为空，回到根菜单
			currentMenu = rootMenu;
			updateMenuDisplay();
		}
	}

	/**
	 * 更新菜单显示
	 */
	private void updateMenuDisplay() {
		try {
			updateTeamList(false);
		} catch (IOException e) {
			Log.error("Failed to update menu display", e);
		}
	}

	/**
	 * 获取当前菜单选项列表
	 */
	private List<MenuOption> getCurrentMenuOptions() {
		return currentMenu;
	}

	/**
	 * 更新队伍列表。
	 * 
	 * @param simpleMode 简单模式(1.14+).减少网络数据通信。
	 * @throws IOException
	 */
	public void updateTeamList(boolean simpleMode) throws IOException {
		GameOutputStream o = new GameOutputStream();
		// log.d("Sending teamlist...");
		o.writeInt(-1);// player.index
		// 1.14新增
		o.writeBoolean(simpleMode);
		o.writeInt(MAX_MENU_SIZE); // maxPlayer
		// 1.14启用Gzip压缩
		GzipEncoder enc = o.getEncodeStream("teams", true);

		for (Byte i = 0; i < MAX_MENU_SIZE; i++) {
			MenuOption options;
			if (i < currentMenu.size()) {
				enc.stream.writeBoolean(true);// 原先表示是否有效玩家
				options = currentMenu.get(i);
			} else {
				enc.stream.writeBoolean(false);// 原先表示是否有效玩家
				continue;
			}

			// 1.14
			// enc.stream.writeByte(0);

			enc.stream.writeInt(255);
			// Log.info(options.index);
			writeMenuOptions(enc.stream, simpleMode, options);
		}
		o.flushEncodeData(enc);

		o.writeInt(currectRoom.config.fogType);
		o.writeInt(GameUtils.getMoneyFormat(currectRoom.config.credits));
		o.writeBoolean(true);
		// ai
		o.writeInt(1);
		//
		o.writeByte(4);
		// maxUnit
		o.writeInt(250);
		o.writeInt(250);

		// 初始单位
		o.writeInt(currectRoom.config.startingUnits);
		o.writeFloat(currectRoom.config.income);
		o.writeBoolean(currectRoom.config.disableNuke);
		o.writeBoolean(false);
		o.writeBoolean(false);
		o.writeBoolean(currectRoom.config.sharedControl);

		Packet p = o.createPacket(PacketType.TEAM_LIST);

		handler.ctx.writeAndFlush(p);
	}

	public void writeMenuOptions(DataOutputStream stream, boolean simpleMode, MenuOption options)
			throws IOException {
		if (simpleMode) {
			stream.writeByte(0);
			stream.writeInt(options.ping);// 原显示延迟
			stream.writeBoolean(false);// isSharingControl
			stream.writeBoolean(false);// isDisconnected || isAfk
		} else {
			// 玩家位置
			stream.writeByte(options.index);
			// 玩家资金(毫无作用)
			stream.writeInt(0);
			// 玩家队
			stream.writeInt(options.team);

			stream.writeBoolean(true);

			stream.writeUTF(options.name);

			stream.writeBoolean(true);

			// enc.stream.writeBoolean(true);
			stream.writeInt(options.ping);// 原显示延迟
			stream.writeLong(System.currentTimeMillis());

			// 是否AI
			stream.writeBoolean(false);
			// AI难度
			stream.writeInt(0);

			// 玩家队伍
			stream.writeInt(options.team);
			stream.writeByte(0);

			// 分享控制
			stream.writeBoolean(false);
			// 是否掉线
			stream.writeBoolean(false);

			// 是否投降
			stream.writeBoolean(false);
			stream.writeBoolean(false);
			stream.writeInt(-9999);

			stream.writeBoolean(false);
			// 是否房主
			stream.writeInt(0);

			// 1.15新增
			stream.writeBoolean(false);
			stream.writeBoolean(false);
			stream.writeBoolean(false);
			stream.writeBoolean(false);

			// color - playerIndex
			stream.writeInt(options.index);
		}
	}

	private class MenuOption {
		public byte optionId;// start by 1 因为0是玩家本身
		public int index;
		public int team;
		public int ping = 0;
		public String name;
		Callback callback;

		public interface Callback {
			void onClick();// 实际应该算是onKick
		}
	}

	public void chatEvent(String msg) {
		if (msg.startsWith(".") || msg.startsWith("-") || msg.startsWith("_")) {
			String cmd = msg.substring(1);
			while (true) {
				if (cmd.startsWith("qc")) {
					cmd = cmd.substring(4);
					break;
				}
			}
			Log.info("final cmd is:" + cmd);

			// 处理菜单点击事件
			List<MenuOption> currentOptions = getCurrentMenuOptions();
			for (MenuOption option : currentOptions) {
				if (cmd.equals("kick " + (option.index + 1))) {// 反正就是要+1
					option.callback.onClick();
					return; // 找到匹配项后立即返回
				}
			}

			// 如果没有找到匹配的菜单项，可以在这里处理其他命令
		} else {
			try {
				sendPacket(UniversalPacket.chat("YOUR", msg, 0));
			} catch (IOException e) {
				Log.error("Failed to send chat message", e);
			}
		}
	}

	// 其余方法保持不变...
	/**
	 * 心跳包任务
	 * Ping runnable.
	 */
	public class PingTasker implements Runnable {
		@Override
		public void run() {
			try {
				GameOutputStream o = new GameOutputStream();
				o.writeLong(new Random().nextLong());
				o.writeByte(0);
				Packet p = o.createPacket(108);
				handler.ctx.writeAndFlush(p);
				pingTime = System.currentTimeMillis();
			} catch (IOException e) {
				stopPingTask();
			}
		}
	}

	/**
	 * 队伍列表任务
	 * TeamTask Scheduler.
	 */
	public class TeamTasker implements Runnable {
		@Override
		public void run() {
			try {
				updateTeamList(false);
			} catch (IOException e) {
				stopTeamTask();
				// log.e(e);
				// e.printStackTrace();
				// cancel();
			}
		}
	}

	public void startPingTask() {
		if (pingFuture != null)
			return;
		pingFuture = Rukkit.getThreadManager().schedule(new PingTasker(), 2000, 2000);
	}

	public void startTeamTask() {
		if (teamFuture != null)
			return;
		teamFuture = Rukkit.getThreadManager().schedule(new TeamTasker(), 1000, 1000);
	}

	public void stopPingTask() {
		if (pingFuture == null)
			return;
		Rukkit.getThreadManager().shutdownTask(pingFuture);
		pingFuture = null;
	}

	public void stopTeamTask() {
		if (teamFuture == null)
			return;
		Rukkit.getThreadManager().shutdownTask(teamFuture);
		teamFuture = null;
	}

	/**
	 * 发送服务器信息 ([SERVER])
	 * 
	 * @param msg
	 */
	public void sendServerMessage(String msg) {
		try {
			handler.ctx.writeAndFlush(UniversalPacket.chat("SERVER", msg, -1));
		} catch (IOException e) {
		}
	}

	/**
	 * 踢出玩家
	 * 
	 * @param reason 踢出理由
	 */
	public void kick(String reason) {
		try {
			handler.ctx.writeAndFlush(UniversalPacket.kick(reason));
		} catch (IOException e) {
		}
	}

	/**
	 * 向连接发送数据包并刷新
	 * 
	 * @param reason 数据包
	 */
	public void sendPacket(Packet packet) {
		handler.ctx.writeAndFlush(packet);
	}

	/**
	 * 心跳包返回
	 */
	public void pong() {
		// player.ping = (int) (System.currentTimeMillis() - pingTime);
	}
}