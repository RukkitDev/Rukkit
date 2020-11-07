package io.rukkit.command;
import io.netty.channel.*;
import io.rukkit.*;
import io.rukkit.entity.*;
import io.rukkit.map.*;
import io.rukkit.net.*;
import io.rukkit.util.*;
import java.util.*;
import io.netty.channel.group.*;
import java.io.*;
import io.rukkit.event.player.*;

public class ChatCommand
{
	private static Logger log = new Logger("Command");
	public static AfkTask afkTask = new AfkTask(null, null);
	public static class AfkTask extends TimerTask
	{
		public boolean isAfk = false;
		int afkTime = 0;
		//PlayerThread targetThread;
		public Player from,to;
		
		public AfkTask(Player from, Player to){
			//this.targetThread = thread;
			this.from = from;
			this.to = to;
		}
		
		public void breakDown() throws IOException{
			if(isAfk){
				ChannelGroups.broadcast(new Packet().chat("Server", "转移取消...", -1));
				cancel();
				isAfk = false;
			}
		}
		
		@Override
		public void run()
		{
			isAfk = true;
			try{
			// TODO: Implement this method
			afkTime+=1;
			if(afkTime == 15){
				ChannelGroups.broadcast(new Packet().chat("Server", "15 秒后转移管理员...", -1));
				//Rukkit.thread.sendSystemBoardcast("15 秒后转移管理员...");
			}
			if(afkTime == 30){
				ChannelGroups.broadcast(new Packet().chat("Server", "管理员已转移...", -1));
				from.isAdmin = false;
				to.isAdmin = true;
				ChannelGroups.broadcast(new Packet().serverInfo());
				to.ctx.writeAndFlush(new Packet().serverInfo(true));
				isAfk = false;
				cancel();
				/*Rukkit.thread.sendSystemBoardcast("管理员已转移！");
				Rukkit.thread.player.setAdmin(targetThread.threadIndex, true);
				cancel();*/
			}
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		
	}
	public static void executeCommand(String command, ChannelHandlerContext thread, Player p){
		log.d("Executed:" + command);
		String cmd[] = command.split(" ");
		try{
			log.d(cmd[0]);
			switch(cmd[0]){
				case "qc":
					executeCommand(command.substring(4), thread, p);
					break;
				case "self_move":
					if(GameServer.isGaming()){
						return;
					}
					log.d("Player Moved");
					try{
						
						if(p.movePlayer(Integer.parseInt(cmd[1]) - 1)){
							thread.writeAndFlush(new Packet().chat("Server", "移动成功!", -1));
						}else{
							thread.writeAndFlush(new Packet().chat("Server", "失败：位置不存在或者有人在位置上!", -1));
						}
					}catch(Exception e){
						e.printStackTrace();
					}
					break;
				case "self_team":
					if(GameServer.isGaming()){
						return;
					}
					p.moveTeam(Integer.parseInt(cmd[1]) - 1);
					break;
					/*
					if(Rukkit.thread.isGaming){
						thread.sendSystemMessage("游戏已经启动！");
						return;
					}
					if(thread.threadIndex > ServerProperties.maxPlayer - 1){
						thread.sendSystemMessage("观战中不允许移动队伍！");
						return;
					}
					log.i("Player team Moved");
					try{
						Player player = Rukkit.thread.player.fetchPlayer(thread.threadIndex);
						if(!player.moveTeam(Integer.parseInt(cmd[1]) - 1)){
							thread.sendSystemMessage("操作失败：队伍太大或太小或你正在观战");
						}else{
							thread.updateTeamList();
						}
					}catch(Exception e){
						e.printStackTrace();
					}
					break;*/
				case "t":
					for(Player s: PlayerGroup.getPlayers()){
						try{
						if(s.playerTeam == p.playerTeam){
							StringBuffer sbuf = new StringBuffer(cmd[1]);
							for(int i = 2;i< cmd.length; i++){
								sbuf.append(" " + cmd[i]);
							}
							s.ctx.writeAndFlush(new Packet().chat(p.playerName, "[团队消息(TEAM)] " + sbuf, p.playerIndex));
						}
						}catch(NullPointerException e){}
					}
					break;
				case "start":
					if(GameServer.tickTime >= 0){
						thread.writeAndFlush(new Packet().chat("Server", "游戏已经开始!", -1));
						return;
					}
					if(ChannelGroups.size() < ServerProperties.minStartPlayer){
						thread.writeAndFlush(new Packet().chat("Server", "至少需要" + ServerProperties.minStartPlayer + "人才可以开始游戏！", -1));
						thread.writeAndFlush(new Packet().chat("Server", "Must have" + ServerProperties.minStartPlayer + " players！", -1));
						return;
					}
					Rukkit.game.startGame();
					break;
				case "afk":
					if(p.isAdmin){
						thread.writeAndFlush(new Packet().chat("Server", "你已经是管理员了！", -1));
						return;
					}
					try{
					if(afkTask.isAfk){
						thread.writeAndFlush(new Packet().chat("Server", "已经有转移开始了！", -1));
						return;
					}}catch(NullPointerException e){}
					afkTask = new AfkTask(PlayerGroup.getAdmin(), p);
					new Timer().schedule(afkTask, 0, 1000);
					ChannelGroups.broadcast(new Packet().chat("Server", "转移开始！", -1));
					break;
				case "break":
					
					break;
				case "give":
					log.d("executed.");
					if(GameServer.isGaming()){
						return;
					}
					if(!p.isAdmin){
						return;
					}
					p.isAdmin = false;
					try{
					PlayerGroup.get(Integer.parseInt(cmd[1]) - 1).toString();
					PlayerGroup.get(Integer.parseInt(cmd[1]) - 1).isAdmin = true;
					ChannelGroups.broadcast(new Packet().serverInfo());
					PlayerGroup.get(Integer.parseInt(cmd[1]) - 1).ctx.writeAndFlush(new Packet().serverInfo(true));
					}catch(NullPointerException e){
						p.isAdmin = true;
					}
					log.d("given.");
					break;
				case "maps":
					StringBuffer buf = new StringBuffer();
					for(int i=OfficalMap.maps.length - 1;i>=0;i--){
						buf.append(String.format("[%d] %s", i, OfficalMap.maps[i]) + "\n");
					}
					thread.writeAndFlush(new Packet().chat("Server", "======== 地图列表 Map List =======\n" +
															"管理员发送 .map 地图序号 即可换图！\n" +
															buf, -1));
					break;
				case "map":
					if(!p.isAdmin){
						return;
					}
					StringBuffer sbuf = new StringBuffer(cmd[1]);
					for(int i = 2;i< cmd.length; i++){
						sbuf.append(" " + cmd[i]);
					}
					log.d(sbuf.toString());
					if(sbuf.toString().startsWith("'")){
						String mapString = sbuf.toString().split("'")[1];
						for(int i=0;i<OfficalMap.mapsName.length;i++){
							if(OfficalMap.mapsName[i].contains(mapString)){
								ServerProperties.mapName = OfficalMap.maps[i];
								ServerProperties.mapType = 0;
								ChannelGroups.broadcast(new Packet().serverInfo());
								thread.writeAndFlush(new Packet().serverInfo(true));
								break;
							}
						}
						//ChannelGroups.broadcast(new Packet().chat(p.playerName, "-map " + cmd[1], p.playerIndex));
						return;
					}
					try{
						ServerProperties.mapName = OfficalMap.maps[Integer.parseInt(cmd[1])];
						ServerProperties.mapType = 0;
						ChannelGroups.broadcast(new Packet().serverInfo());
						thread.writeAndFlush(new Packet().serverInfo(true));
						ChannelGroups.broadcast(new Packet().chat(p.playerName, "-map " + cmd[1], p.playerIndex));
					}catch(Exception e){
						
					}
					thread.writeAndFlush(new Packet().serverInfo(true));
					break;
				case "income":
					if(GameServer.isGaming()){
						return;
					}
					Float income = Float.parseFloat(cmd[1]);
					if(income <= 0 || income > 100){
						return;
					}
					ServerProperties.income = income;
					break;
				case "auto_team":
					
					break;
				case "stop":
					
					break;
				case "help":
					thread.writeAndFlush(new Packet().chat("Server","\n===== 帮助 =====\n" +
											 "[管理员指令]\n" +
											 ".start 开始游戏\n" +
											 ".maps 显示地图列表\n" +
											 ".map 地图序号 切换地图\n" +
											 ".income 倍数 切换倍数\n" +
											 ".auto_team 自动分队\n" +
											 ".give 玩家位 把自己的管理给位置上的玩家\n" +
											 ".kick 玩家位 把某号玩家踢出\n" +
											 "[玩家指令]\n" +
											 ".stop 在游戏准备开始时停止游戏\n" +
											 ".stat 查看目前是否开始游戏\n" +
											 ".watch 玩家位 观战该玩家\n" +
											 ".unwatch 取消观战模式并回到玩家位\n" +
											 ".afk 获得管理\n" +
											 ".break 打破管理获得计时\n" +
											 ".who 查看谁是管理", -1));
					break;
					/*
				case "stat":
					if(!Rukkit.thread.isGaming){
						thread.sendSystemMessage("游戏未开始！");
					}
					thread.sendSystemMessage("当前正在的游戏玩家人数：" + Rukkit.thread.player.totalPlayers());
					break;
				case "watch":
					if(Rukkit.thread.isGaming){
						thread.sendSystemMessage("游戏已经启动！");
						return;
					}
					if(thread.threadIndex > (ServerProperties.maxPlayer - 1)){
						if(Rukkit.thread.player.fetchPlayer(Integer.parseInt(cmd[1])-1) != null && thread.threadIndex != Integer.parseInt(cmd[1])-1){
							Rukkit.thread.player.fetchPlayer(thread.threadIndex).playerIndex = Integer.parseInt(cmd[1])-1;
							Rukkit.thread.player.fetchPlayer(thread.threadIndex).playerTeam = Rukkit.thread.player.fetchPlayer(Integer.parseInt(cmd[1])-1).playerTeam;
							thread.sendSystemMessage("您正在观战 " + Rukkit.thread.player.fetchPlayer(Integer.parseInt(cmd[1])-1).playerName + "！");
						}else{
							thread.sendSystemMessage("您想要观战的玩家不存在或者是你自己！");
						}
					}else{
						if(Rukkit.thread.player.totalWatchers() >= ServerProperties.maxWatcher){
							thread.sendSystemMessage("观战者已满！");
							return;
						}
						Player player = Rukkit.thread.player.fetchPlayer(thread.threadIndex);
						Rukkit.thread.player.deletePlayer(thread.threadIndex);
						thread.threadIndex =  Rukkit.thread.player.addWatcher(player.playerName);
						thread.sendSystemMessage("您已被切换至观战位！");
						if(Rukkit.thread.player.fetchPlayer(Integer.parseInt(cmd[1])-1) != null&& thread.threadIndex != Integer.parseInt(cmd[1])-1){
							Rukkit.thread.player.fetchPlayer(thread.threadIndex).playerIndex = Integer.parseInt(cmd[1])-1;
							Rukkit.thread.player.fetchPlayer(thread.threadIndex).playerTeam = Rukkit.thread.player.fetchPlayer(Integer.parseInt(cmd[1])-1).playerTeam;
							thread.sendSystemMessage("您正在观战 " + Rukkit.thread.player.fetchPlayer(Integer.parseInt(cmd[1])-1).playerName + "！");
						}else{
							thread.sendSystemMessage("您想要观战的玩家不存在或者是你自己！");
						}
					}
					break;
				case "unwatch":
					if(Rukkit.thread.isGaming){
						thread.sendSystemMessage("游戏已经启动！");
						return;
					}
					if(thread.threadIndex < ServerProperties.maxPlayer - 1){
						thread.sendSystemMessage("您已是非观战状态！");
						return;
					}
					Player player = Rukkit.thread.player.fetchPlayer(thread.threadIndex);
					int threadIndex =  Rukkit.thread.player.addPlayer(player.playerName);
					if(threadIndex == -1){
						thread.sendSystemMessage("玩家人数已满！");
						return;
					}
					Rukkit.thread.player.deletePlayer(thread.threadIndex);
					thread.threadIndex = threadIndex;
					thread.sendSystemMessage("取消观战成功！");
					break;
				case "autofix":
					Rukkit.thread.sendSystemBoardcast("有人启动了自检模式！");
					Rukkit.thread.sendSystemBoardcast("检查玩家存活状态...");
					for(PlayerThread s : Rukkit.thread.clients){
						try{
							s.ping();
						}catch(IOException e){
							s.disconnect();
						}
					}
					Rukkit.thread.sendSystemBoardcast("检查完成！");
					break;*/
				case "kick":
					if(GameServer.isGaming()){
						return;
					}
					if(p.isAdmin){
						PlayerGroup.get(Integer.parseInt(cmd[1]) - 1).ctx.writeAndFlush(new Packet().kick("Kicked by Admin.\n你被管理员踢出！"));
						//Rukkit.thread.clients.get().sendKick("被房主踢出!");
					}
					break;
				case "cmaps":
					StringBuffer buff = new StringBuffer();
					ArrayList li = CustomMapLoader.getMapNameList();
					for(int i=li.size() - 1;i>=0;i--){
						buff.append(String.format("[%d] %s", i, li.get(i)) + "\n");
					}
					thread.writeAndFlush(new Packet().chat("Server", "======== 地图列表 Map List =======\n" +
														   "管理员发送 .map 地图序号 即可换图！\n" +
														   buff, -1));
														   break;
				case "cmap":
					try{
						ServerProperties.mapName = CustomMapLoader.getMapNameList().get(Integer.parseInt(cmd[1]));
						ServerProperties.mapType = 1;
						ChannelGroups.broadcast(new Packet().serverInfo());
						ChannelGroups.broadcast(new Packet().chat(p.playerName, "-cmap " + cmd[1], p.playerIndex));
					}catch(Exception e){

					}
					break;
				case "nukes":
					if(GameServer.isGaming()){
						return;
					}
					if(!p.isAdmin){
						return;
					}
					//log.d(!Boolean.parseBoolean(cmd[1]));
					ServerProperties.disableNuke = !Boolean.parseBoolean(cmd[1]);
					//log.d(ServerProperties.disableNuke);
					//log.d("Nukes set.");
					ChannelGroups.broadcast(new Packet().serverInfo());
					thread.writeAndFlush(new Packet().serverInfo(true));
					break;
				case "sharedControl":
					if(GameServer.isGaming()){
						return;
					}
					if(!p.isAdmin){
						return;
					}
					//log.d(!Boolean.parseBoolean(cmd[1]));
					ServerProperties.sharedControl = Boolean.parseBoolean(cmd[1]);
					//log.d(ServerProperties.disableNuke);
					//log.d("Nukes set.");
					break;
				case "credits":
					if(GameServer.isGaming()){
						return;
					}
					if(!p.isAdmin){
						return;
					}
					//log.d(!Boolean.parseBoolean(cmd[1]));
					ServerProperties.credits = Integer.parseInt(cmd[1]);
					//log.d(ServerProperties.disableNuke);
					//log.d("Nukes set.");
					break;
				case "fog":
					if(GameServer.isGaming()){
						return;
					}
					if(!p.isAdmin){
						return;
					}
					//log.d(!Boolean.parseBoolean(cmd[1]));
					switch(cmd[1]){
						case "off":
							ServerProperties.fogType = 0;
							break;
						case "basic":
							ServerProperties.fogType = 1;
							break;
						case "los":
							ServerProperties.fogType = 2;
							break;
						default:
							ServerProperties.fogType = 2;
					}
					ChannelGroups.broadcast(new Packet().serverInfo());
					thread.writeAndFlush(new Packet().serverInfo(true));
					//ServerProperties.sharedControl = Boolean.parseBoolean(cmd[1]);
					//log.d(ServerProperties.disableNuke);
					//log.d("Nukes set.");
					break;
				case "startingunits":
					if(GameServer.isGaming()){
						return;
					}
					if(!p.isAdmin){
						return;
					}
					//log.d(!Boolean.parseBoolean(cmd[1]));
					ServerProperties.startingUnits = Integer.parseInt(cmd[1]);
					ChannelGroups.broadcast(new Packet().serverInfo());
					thread.writeAndFlush(new Packet().serverInfo(true));
					//log.d(ServerProperties.disableNuke);
					//log.d("Nukes set.");
					break;
				case "team":
					if(GameServer.isGaming()){
						return;
					}
					if(!p.isAdmin){
						return;
					}
					PlayerGroup.get(Integer.parseInt(cmd[1]) - 1).playerTeam = Integer.parseInt(cmd[2]) - 1;
					break;
				case "move":
					if(GameServer.isGaming()){
						return;
					}
					if(!p.isAdmin){
						return;
					}
					Player fromPlayer = PlayerGroup.get(Integer.parseInt(cmd[1]) - 1);
					Player targetPlayer = PlayerGroup.get(Integer.parseInt(cmd[2]) - 1);
					try{
						if(fromPlayer.movePlayer(Integer.parseInt(cmd[2]) - 1)){
							thread.writeAndFlush(new Packet().chat("Server", "移动成功!", -1));
						}else{
							int fromslot, toslot;
							fromslot = fromPlayer.playerIndex;
							toslot = targetPlayer.playerIndex;
							PlayerGroup.remove(targetPlayer);
							fromPlayer.movePlayer(toslot);
							targetPlayer.movePlayer(fromslot);
						}
					}catch(Exception e){
						//fromPlayer.movePlayer(Integer.parseInt(cmd[1]) - 1);
						e.printStackTrace();
					}
					break;
				case "share":
					if(ServerProperties.sharedControl == false){
						return;
					}
					switch(cmd[1]){
						case "on":
							p.isSharingControl = true;
							ChannelGroups.broadcast(new Packet().chat("Server", p.playerName + " 启动了共享控制！", -1));
							break;
						case "off":
							p.isSharingControl = false;
							ChannelGroups.broadcast(new Packet().chat("Server", p.playerName + " 关闭了共享控制！", -1));
							break;
						default:
						p.isSharingControl = false;
							ChannelGroups.broadcast(new Packet().chat("Server", p.playerName + " 关闭了共享控制！", -1));
					}
					break;
				case "version":
					thread.writeAndFlush(new Packet().chat("Server", "Rukkit ver1.1.0 by wtbdev", -1));
					break;
				default:
					PlayerCommandEvent.getListenerList().callListeners(new PlayerCommandEvent(p, command));
					//Rukkit.getCurrentPluginHandler().onChatRecieve(p, command);
			}
		}catch(Exception e){
			//e.printStackTrace();
			//thread.sendSystemMessage("参数有误！");
		}
	}
}
