package cn.rukkit.plugin.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.stream.Collectors;

import cn.rukkit.Rukkit;
import cn.rukkit.command.*;
import cn.rukkit.event.EventListener;
import cn.rukkit.plugin.PluginConfig;
import cn.rukkit.util.LangUtil;

public class UplistCommandPlugin extends InternalRukkitPlugin implements EventListener {
    // Configuration
    private static final List<String> MASTER_SERVER_URLS = List.of(
            "http://gs4.corrodinggames.net:80/masterserver/1.4",
            "http://gs1.corrodinggames.com/masterserver/1.4"
    );
    private static String SERVER_ID = "u_3c45cbc1-37a-457f-95de-46b271eb151";
    private static final String SERVER_NAME = "Unnamed";
    private static final int SERVER_PORT = 5123;
    private static final String CREATED_BY = "🇨🇳两岸服务器 [测试中/未开放]";
    private static final String MAP_NAME = "Tow Side (8P)";
    private static final long UPDATE_INTERVAL = 60000; // 60 seconds

    // Timer for scheduled updates
    private Timer updateTimer;
    private boolean updateServiceEnabled = false;

    public static void main(String[] args) {
        addToPublicList();
    }

    public static String generateRandomId() {
        // 生成标准UUID并移除连字符
        String uuid = UUID.randomUUID().toString().replace("-", "");

        // 构建自定义格式：u_8-3-4-4-12
        return String.format("u_%s-%s-%s-%s-%s",
                uuid.substring(0, 8), // 8字符
                uuid.substring(8, 11), // 3字符
                uuid.substring(11, 15), // 4字符
                uuid.substring(15, 19), // 4字符
                uuid.substring(19, 30) // 剩余12字符
        );
    }

    private static void addToPublicList() {
        String postData = "action=add" +
                "&user_id=" + SERVER_ID +
                "&_1=1752825498133" +
                "&tx2=86C7" +
                "&tx3=86EC" +
                "&game_name=" + encodeValue(SERVER_NAME) +
                "&game_version=176" +
                "&game_version_string=1.15" +
                "&game_version_beta=false" +
                "&private_token=m9n41crnvw80vtxmfqg1nop25fnojz132pgraiui" +
                "&private_token_2=62eda904ede2980dcb1fa3aa9b1b837c" +
                "&confirm=4d49d5743fb0b92e0abe6a988acdb7cb" +
                "&password_required=false" +
                "&created_by=" + encodeValue(CREATED_BY) +
                "&private_ip=8.138.146.146" +
                "&port_number=" + SERVER_PORT +
                "&game_map=" + encodeValue(MAP_NAME) +
                "&game_mode=skirmishMap" +
                "&game_status=battleroom" +
                "&player_count=1" +
                "&max_player_count=10";

        sendToAllServers(postData, "Adding to public list");
    }

    private static void updatePublicList() {
        String postData = "action=update" +
                "&id=" + SERVER_ID +
                "&private_token=m9n41crnvw80vtxmfqg1nop25fnojz132pgraiui" +
                "&password_required=false" +
                "&created_by=" + encodeValue(CREATED_BY) +
                "&private_ip=8.138.146.146" +
                "&port_number=" + SERVER_PORT +
                "&game_map=" + encodeValue(MAP_NAME) +
                "&game_mode=skirmishMap" +
                "&game_status=battleroom" +
                "&player_count=1" +
                "&max_player_count=10";

        sendToAllServers(postData, "Updating public list");
    }

    private static void removeFromPublicList() {
        String postData = "action=remove" +
                "&id=" + SERVER_ID +
                "&private_token=m9n41crnvw80vtxmfqg1nop25fnojz132pgraiui";

        sendToAllServers(postData, "Removing from public list");
    }

    private static void sendToAllServers(String postData, String actionDescription) {
        for (String url : MASTER_SERVER_URLS) {
            String apiUrl = url + "/interface";
            System.out.println(actionDescription + " on: " + apiUrl);

            try {
                String response = sendRequest(apiUrl, postData);
                if (response != null && !response.isEmpty()) {
                    System.out.println("Response from server:");
                    System.out.println(response);
                } else {
                    System.out.println("Failed to get response from " + url);
                }
            } catch (IOException e) {
                System.out.println("Error " + actionDescription.toLowerCase() + " on " + url + ": " + e.getMessage());
            }
        }
    }

    private static String sendRequest(String urlString, String postData) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = null;

        try {

            connection = (HttpURLConnection) url.openConnection();

            // 超时设置（必须）
            connection.setConnectTimeout(15000); // 15秒连接超时
            connection.setReadTimeout(30000); // 30秒读取超时

            // 通用请求头
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("Connection", "keep-alive");

            // POST请求处理
            if (postData != null) {
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(postData.getBytes(StandardCharsets.UTF_8));
                }
            } else {
                connection.setRequestMethod("GET");
            }

            // 自动处理重定向（最多5次）
            connection.setInstanceFollowRedirects(false); // 手动控制更安全
            int redirectCount = 0;
            int responseCode;
            String response;

            do {
                responseCode = connection.getResponseCode();

                // 统一读取响应内容（包括错误流）
                response = readResponse(connection);

                // 处理重定向
                if (responseCode >= 300 && responseCode < 400) {
                    String newUrl = connection.getHeaderField("Location");
                    if (newUrl == null || redirectCount++ >= 5) {
                        break;
                    }
                    connection = (HttpURLConnection) new URL(newUrl).openConnection();
                    continue;
                }

                // 非成功响应抛出包含详细信息的异常
                if (responseCode < 200 || responseCode >= 300) {
                    String errorHeaders = getResponseHeaders(connection);
                    throw new IOException(String.format(
                            "HTTP请求失败\n状态码: %d\n响应头: %s\n响应体: %s",
                            responseCode, errorHeaders, response));
                }

                return response;

            } while (true);

            throw new IOException("重定向次数超过限制");

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // 统一读取响应内容（包括错误流）
    private static String readResponse(HttpURLConnection conn) throws IOException {
        InputStream inputStream = conn.getResponseCode() < 400 ? conn.getInputStream() : conn.getErrorStream();

        if (inputStream == null) {
            return "";
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    // 获取响应头信息
    private static String getResponseHeaders(HttpURLConnection conn) {
        return conn.getHeaderFields().entrySet().stream()
                .filter(entry -> entry.getKey() != null)
                .map(entry -> entry.getKey() + ": " + String.join(", ", entry.getValue()))
                .collect(Collectors.joining("\n"));
    }

    private static String encodeValue(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return value;
        }
    }

    @Override
    public void onEnable() {
        getLogger().info("Uplist Server Command Plugin enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("Uplist Server Command Plugin disabled");
        stopUpdateService();
        removeFromPublicList();
    }

    @Override
    public void onStart() {
        getLogger().info("Uplist Server Command Plugin started");
    }

    @Override
    public void onDone() {
        getLogger().info("Uplist Server Command Plugin finished");
    }

    @Override
    public void loadConfig() {
        config = new PluginConfig();
        config.name = "[Extension] Uplist Server Command Plugin";
        config.author = "micro";
        config.id = "uplist-server-command-plugin";
        config.version = Rukkit.RUKKIT_VERSION;
        config.pluginClass = "cn.rukkit.plugin.internal.UplistCommandPlugin";
        config.apiVersion = Rukkit.PLUGIN_API_VERSION;
    }

    @Override
    public void onLoad() {
        getLogger().info("Uplist Server Command Plugin[Extension]::onLoad...");
        CommandManager mgr = Rukkit.getCommandManager();
        Rukkit.getPluginManager().registerEventListener(this, this);
        mgr.registerServerCommand(
                new ServerCommand("uplist", LangUtil.getString("chat.debug"), 1, new UplistCallback(), this));
        mgr.registerServerCommand(new ServerCommand("uplistSvc", LangUtil.getString("chat.debug"), 1,
                new UplistAndEnableUpdateServiceCallback(), this));
        mgr.registerServerCommand(
                new ServerCommand("downlist", LangUtil.getString("chat.debug"), 1, new DownlistCallback(), this));
        mgr.registerServerCommand(new ServerCommand("EnableUpdateService", "Enable periodic server list updates", 1,
                new EnableUpdateServiceCallback(), this));
        mgr.registerServerCommand(new ServerCommand("DisableUpdateService", "Disable periodic server list updates", 1,
                new DisableUpdateServiceCallback(), this));

        getLogger().info("ServerID[old]" + SERVER_ID);
        SERVER_ID = generateRandomId();
        getLogger().info("ServerID[new]" + SERVER_ID);
    }

    private void startUpdateService() {
        if (updateServiceEnabled) {
            getLogger().info("Update service is already running");
            return;
        }

        updateTimer = new Timer("ServerListUpdater", true);
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    getLogger().info("Executing scheduled server list update...");
                    updatePublicList();
                } catch (Exception e) {
                    getLogger().error("Error during scheduled update", e);
                }
            }
        }, 0, UPDATE_INTERVAL);

        updateServiceEnabled = true;
        getLogger().info("Started update service with interval: " + UPDATE_INTERVAL + "ms");
    }

    private void stopUpdateService() {
        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer = null;
        }
        updateServiceEnabled = false;
        getLogger().info("Stopped update service");
    }

    class UplistCallback implements ServerCommandListener {
        @Override
        public void onSend(String[] args) {
            getLogger().info("Adding server to public list...");
            addToPublicList();
            getLogger().info("Server added to public list successfully");
        }
    }

    class UplistAndEnableUpdateServiceCallback implements ServerCommandListener {
        @Override
        public void onSend(String[] args) {
            getLogger().info("Adding server to public list...");
            addToPublicList();
            getLogger().info("Server added to public list successfully");
            getLogger().info("Enabling update service...");
            startUpdateService();
            getLogger().info("Update service enabled");
        }
    }

    class DownlistCallback implements ServerCommandListener {
        @Override
        public void onSend(String[] args) {
            getLogger().info("Removing server from public list...");
            removeFromPublicList();
            getLogger().info("Server removed from public list successfully");
        }
    }

    class EnableUpdateServiceCallback implements ServerCommandListener {
        @Override
        public void onSend(String[] args) {
            getLogger().info("Enabling update service...");
            startUpdateService();
            getLogger().info("Update service enabled");
        }
    }

    class DisableUpdateServiceCallback implements ServerCommandListener {
        @Override
        public void onSend(String[] args) {
            getLogger().info("Disabling update service...");
            stopUpdateService();
            getLogger().info("Update service disabled");
        }
    }
}