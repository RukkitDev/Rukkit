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
import cn.rukkit.command.ChatCommandListener;
import cn.rukkit.network.RoomConnection;
import cn.rukkit.plugin.PluginConfig;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;

public class ListPublishPlugin extends InternalRukkitPlugin {

    private static final String allChar = "0123456789abcdefghijklmnopqrstuvwxyz";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.132 Safari/537.36";

    private static String token;

    private Logger log = LoggerFactory.getLogger(ListPublishPlugin.class);

    // 新版实现 随机40个长度
    public static String generateStr(int len){
        StringBuffer sb = new StringBuffer();
        Random random = new Random();
        for (int i = 0; i <len ; i++) {
            sb.append(allChar.charAt(random.nextInt(allChar.length())));
        }
        return sb.toString();
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

    private void addServer() throws Exception {
        token = generateStr(40);
        log.info(doPost("http://gs4.corrodinggames.net/masterserver/1.3/interface","action=self_info&port="+ Rukkit.getConfig().serverPort));
        StringBuffer sb = new StringBuffer();
        // 创建ADD
        sb.append("action=add")
                // UUID
                .append("&user_id=u_"+Rukkit.getConfig().UUID)
                // 用户名前部
                .append("&game_name=unnamed")
                // 版本
                .append("&game_version=151")
                .append("&game_version_string=1.14")
                // tokenv
                .append("&private_token="+token)
                // token2
                .append("&private_token_2="+b(b(token)))
                // 验证
                .append("&confirm="+b("a"+b(token)))
                // 是否携带密码
                .append("&password_required=false")
                // 用户名全称
                .append("&created_by=RUKKIT-NO-STOP")
                // 内网ip
                .append("&private_ip=192.168.0.100")
                // port
                .append("&port_number=" + Rukkit.getConfig().serverPort)
                // 地图名
                .append("&game_map=[随时可进] Rukkit no-stop Server")
                // ?
                .append("&game_mode=skirmishMap")
                // ?
                .append("&game_status=battleroom")
                // 当前玩家数量
                .append("&player_count=" + Rukkit.getGlobalConnectionManager().size())
                // 最大
                .append("&max_player_count=" + (Rukkit.getConfig().maxPlayer));
        log.info(doPost("http://gs4.corrodinggames.net/masterserver/1.3/interface",sb.toString()));
    }

    class PublishTask implements Runnable {
        @Override
        public void run() {
            StringBuffer sb = new StringBuffer();
            String stat = "battleroom";
            // 创建ADD
            sb.append("action=update")
                    .append("&id="+ "u_"+Rukkit.getConfig().UUID)
                    .append("&private_token="+token)
                    .append("&check_port=false")
                    // UUID
                    .append("&user_id=u_"+Rukkit.getConfig().UUID)
                    // 用户名前部
                    .append("&game_name=SERVER")
                    // 版本
                    .append("&game_version=151")
                    .append("&game_version_string=1.14-RK")
                    // token
                    .append("&private_token="+token)
                    // token2
                    .append("&private_token_2="+b(b(token)))
                    // 验证
                    .append("&confirm="+b("a"+b(token)))
                    // 是否携带密码
                    .append("&password_required=false")
                    // 用户名全称
                    .append("&created_by=RUKKIT-NO-STOP")
                    // 内网ip
                    .append("&private_ip=192.168.0.100")
                    // port
                    .append("&port_number=" + Rukkit.getConfig().serverPort)
                    // 地图名
                    .append("&game_map=[随时可进] Rukkit no-stop Server")
                    // ?
                    .append("&game_mode=skirmishMap")
                    // ?
                    .append("&game_status=" + stat)
                    // 当前玩家数量
                    .append("&player_count=" + Rukkit.getGlobalConnectionManager().size())
                    // 最大
                    .append("&max_player_count=" + Rukkit.getConfig().maxPlayer);
            try {
                System.out.println(doPost("http://gs4.corrodinggames.net/masterserver/1.3/interface",sb.toString()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class PublishCommand implements ChatCommandListener {
        @Override
        public boolean onSend(RoomConnection con, String[] args) {
            try {
                addServer();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Rukkit.getThreadManager().schedule(new PublishTask(), 5000, 5000);
            return false;
        }
    }

    private ScheduledFuture publishFuture;

    @Override
    public void onLoad() {

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
        config.pluginClass = "cn.rukkit.plugin.internal.ListPublishPlugin";
        config.name = "ListPublishPlugin";
    }
}
