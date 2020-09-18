package io.rukkit;

import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.*;
import java.net.*;
import io.rukkit.entity.*;
import io.rukkit.net.*;
import io.rukkit.util.*;


public class NetListHelper {
	

	private static final String allChar = "0123456789abcdefghijklmnopqrstuvwxyz";
	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.132 Safari/537.36";
	private static final Logger log = new Logger("NetList");

	private static String uuid;

	private static String token;

	private static int port;
	private static int t = 0;

	// 新版实现 随机40个长度
	public static String generateStr(int len){
		StringBuffer sb = new StringBuffer();
		Random random = new Random();
		for (int i = 0; i <len ; i++) {
			sb.append(allChar.charAt(random.nextInt(allChar.length())));
		}
		return sb.toString();
	}
	
	static TimerTask UpdateTask = new TimerTask(){

		@Override
		public void run()
		{
			/*
			if(t>=40){
				try
				{
					startPublish(port);
					cancel();
				}
				catch (Exception e)
				{}
			}*/
			try
			{
				updateServer(uuid, token);
				t++;
			}
			catch (Exception e)
			{
			}
			// TODO: Implement this method
		}
	};

	public static void startPublish(int port2) throws Exception{
		log.i("Publish Started!");
		token = generateStr(40);
		uuid = UUID.randomUUID().toString();
		port = port2;
		System.out.println(doPost("http://gs4.corrodinggames.net/masterserver/1.3/interface","action=self_info&port="+ port));
		StringBuffer sb = new StringBuffer();
		// 创建ADD
		sb.append("action=add")
		// UUID
		.append("&user_id=u_"+uuid)
		// 用户名前部
		.append("&game_name=unnamed")
		// 版本
		.append("&game_version=136")
		.append("&game_version_string=1.13.3")
		// tokenv
		.append("&private_token="+token)
		// token2
		.append("&private_token_2="+b(b(token)))
		// 验证
		.append("&confirm="+b("a"+b(token)))
		// 是否携带密码
		.append("&password_required=false")
		// 用户名全称
		.append("&created_by=" + ServerProperties.serverUser)
		// 内网ip
		.append("&private_ip=192.168.0.100")
		// port
		.append("&port_number=" + port)
		// 地图名
		.append("&game_map=" + ServerProperties.serverMotd)
		// ?
		.append("&game_mode=skirmishMap")
		// ?
		.append("&game_status=battleroom")
		// 当前玩家数量
		.append("&player_count=" + ChannelGroups.size())
		// 最大
		.append("&max_player_count=" + (ServerProperties.maxPlayer));
		log.d(doPost("http://gs4.corrodinggames.net/masterserver/1.3/interface",sb.toString()));
		log.i("Added.");
		/*System.out.println("\n");
		System.out.println(uuid);*m*/
		new Timer().schedule(UpdateTask, 0, 1000*15);
	}
	
	public static void updateServer(String uuid, String token) throws Exception{
		StringBuffer sb = new StringBuffer();
		String stat = "battleroom";
		if(GameServer.tickTime >= 0){
			stat = "ingame";
		}
		// 创建ADD
		sb.append("action=update")
			.append("&id="+ "u_"+uuid)
			.append("&private_token="+token)
			.append("&check_port=false")
			// UUID
			.append("&user_id=u_"+uuid)
			// 用户名前部
			.append("&game_name=SERVER")
			// 版本
			.append("&game_version=136")
			.append("&game_version_string=1.13.3")
			// token
			.append("&private_token="+token)
			// token2
			.append("&private_token_2="+b(b(token)))
			// 验证
			.append("&confirm="+b("a"+b(token)))
			// 是否携带密码
			.append("&password_required=false")
			// 用户名全称
			.append("&created_by=" + ServerProperties.serverUser)
			// 内网ip
			.append("&private_ip=192.168.0.100")
			// port
			.append("&port_number=" + port)
			// 地图名
			.append("&game_map=" + ServerProperties.serverMotd)
			// ?
			.append("&game_mode=skirmishMap")
			// ?
			.append("&game_status=" + stat)
			// 当前玩家数量
			.append("&player_count=" + ChannelGroups.size())
			// 最大
			.append("&max_player_count=" + (ServerProperties.maxPlayer));
		log.d(doPost("http://gs4.corrodinggames.net/masterserver/1.3/interface",sb.toString()));
		log.i("Updated.");
		/*System.out.println("\n");
		System.out.println(uuid);*/
	}

	public static String doPost(String url, String param) throws Exception {
		String result = "";
		URL realUrl = new URL(url);
		//打开和URL之间的连接
		URLConnection conn =  realUrl.openConnection();
		//设置通用的请求属性
		conn.setRequestProperty("accept", "*/*");
		conn.setRequestProperty("connection", "Keep-Alive");
		conn.setRequestProperty("User-Agent",USER_AGENT);
		//发送POST请求必须设置如下两行
		conn.setDoOutput(true);
		conn.setDoInput(true);
		//获取URLConnection对象对应的输出流
		PrintWriter out = new PrintWriter(conn.getOutputStream());
		//发送请求参数
		out.print(param);
		//flush输出流的缓冲
		out.flush();
		// 定义 BufferedReader输入流来读取URL的响应
		BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
		String line;
		while ((line = in.readLine()) != null) {
			result += "\n" + line;
		}
		return result;
	}

	public static String b(String str) {
		try {
			byte[] digest = MessageDigest.getInstance("MD5").digest(str.getBytes("UTF-8"));
			StringBuilder sb = new StringBuilder(digest.length * 2);
			for (byte b2 : digest) {
				int b3 = b2 & 0xFF;
				if (b3 < 16) {
					sb.append('0');
				}
				sb.append(Integer.toHexString(b3));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e2) {
			throw new RuntimeException("MD5 should be supported", e2);
		} catch (UnsupportedEncodingException e3) {
			throw new RuntimeException("UTF-8 should be supported", e3);
		}
	}

}
