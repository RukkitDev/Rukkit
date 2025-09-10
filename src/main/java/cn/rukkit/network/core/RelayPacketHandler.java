package cn.rukkit.network.core;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.rukkit.network.core.packet.Packet;
import cn.rukkit.network.core.packet.PacketType;
import cn.rukkit.network.core.packet.UniversalPacket;
import cn.rukkit.network.io.GameInputStream;
import cn.rukkit.network.io.GameOutputStream;
import cn.rukkit.network.room.RelayNetworkRoom;
import cn.rukkit.network.room.RelayRoomConnection;
import cn.rukkit.network.room.RelayRoomManager;
import io.netty.channel.ChannelHandlerContext;

// PacketHandler.java
public class RelayPacketHandler extends PacketHandler {
    private static final Logger log = LoggerFactory.getLogger(RelayPacketHandler.class);

    private ChannelHandlerContext ctx;
    private final ConnectionHandler handler;
    private Packet packet;
    private RelayRoomConnection conn;
    private String disconnectReason;
    public boolean host = true;
    private int site = -1;
    public int connectionType = 1;
    private static int clientVersion = 151;
    public static final String SERVER_RELAY_UUID = "Dr (dr@der.kim) & Tiexiu.xyz Core Team";
    // 懒得加一个枚举了 意思如下 以后再改
    /** 链接初始化 */
    // InitialConnection, = 1
    /** 获取链接UUID-Hex */
    // GetPlayerInfo, =2
    /** 等待认证(Pow) */
    // WaitCertified, =3
    /** 认证(Pow)结束 */
    // CertifiedEnd, =4
    /** 向对应房主注册, 但还未进行游戏注册 */
    // PlayerPermission, =5
    /** 向对应房主完成注册, 且游戏完成注册 */

    // PlayerJoinPermission, =6
    /** 该链接为房间 HOST */
    // HostPermission, =7
    private final static boolean multicast = false;

    public RelayPacketHandler(ConnectionHandler handler) {
        this.handler = handler;
        conn = new RelayRoomConnection(handler);
    }

    @Override
    public void updateMsg(ChannelHandlerContext ctx, Object msg) {
        this.ctx = ctx;
        this.packet = (Packet) msg;
    }

    @Override
    public void onConnectionClose(ChannelHandlerContext ctx) {
        log.warn("有一个连接未激活 {}", ctx.toString());
        if (host && conn.currentRoom!=null) {
            RelayRoomManager.removeRoom(conn.currentRoom.roomId);
            for (RelayRoomConnection currConn : conn.currentRoom.connections) {
                //踢出所有玩家
                if(currConn != null){
                    currConn.handler.ctx.close();
                }
            }
        }
        // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method
        // 'onConnectionClose'");
    }

    public void handle() throws Exception {
        conn.pong();//终止超时
        // 检查是否已经注册成为有效连接
        if (relayCheck()) {
            return;
        }

        GameInputStream in = new GameInputStream(packet);
        if (host && connectionType == 7) {
            switch (packet.type) {
                case PacketType.PACKET_FORWARD_CLIENT_TO:
                    // -
                    int target = in.readInt();

                    int type = in.readInt();
                    byte[] bytes = in.readStreamBytes();
                    
                    //提取出目标后再重新封回去
                    GameOutputStream o = new GameOutputStream();
                    o.write(bytes);
                    Packet p = o.createPacket(type);
                    if (conn.currentRoom.connections[target]!=null) {
                        conn.currentRoom.connections[target].handler.ctx.writeAndFlush(p);//发给目标玩家
                    } else {
                        log.warn("企图向不存在的玩家发送封包");
                    }
                    
                    log.info("FOWD TO CL PACKET type{"+type+"} tar"+target);

                    // val abstractNetConnect = room!!.getAbstractNetConnect(target)

                    // Packet(type, bytes).let { sendPacketData ->
                    // abstractNetConnect?.sendPacket(sendPacketData)
                    // sendPacketExtractInformation(sendPacketData, abstractNetConnect)
                    // }
                    break;

            case PacketType.START_GAME:
                conn.currentRoom.isGaming = true;
                if (site == -1 && conn.currentRoom.startGamePacket == null) {
                        in.skip(1);
                        if (in.readInt() != 0) {
                            conn.currentRoom.startGamePacket = packet;
                        }
                        for (RelayRoomConnection currconn : conn.currentRoom.connections) {
                            if(currconn !=null){
                                currconn.handler.ctx.writeAndFlush(packet);
                            }
                        }
                }
                default:
                    break;
            }
        } else {
            switch (packet.type) {
                case PacketType.PLAYER_INFO:
                    relayRegisterConnection(packet);
                    //
                    break;
                case PacketType.DISCONNECT:
                    conn.currentRoom.connections[site]=null;
                    conn.currentRoom.sendPackageToHOST(packet);
                    break;
                default:
                    conn.currentRoom.sendPackageToHOST(packet);
                    break;
            }

        }
    }

    private Boolean relayCheck() throws IOException {
        // Check if connection is already authenticated
        if (connectionType >= 5) {
            return false;
        }

        if (connectionType == 1 && packet.type == PacketType.PREREGISTER_CONNECTION) {
            connectionType = 2;// GetPlayerInfo
            conn.setCachePacket(packet);
            GameOutputStream registerServer = new GameOutputStream();
            registerServer.writeString("net.rwhps.server.relayGetUUIDHex.Dr");// 原先为 SERVER_ID_RELAY_GET
            registerServer.writeInt(1);
            registerServer.writeInt(0);
            registerServer.writeInt(0);
            registerServer.writeString("com.corrodinggames.rts.server");
            registerServer.writeString(SERVER_RELAY_UUID);// SERVER_RELAY_UUID
            registerServer.writeInt("Dr @ 2022".hashCode());
            ctx.writeAndFlush(registerServer.createPacket(PacketType.REGISTER_CONNECTION));
        } else {
            // 原hps debug包 此处不处理 -OLD: conn.exCommand(packet);
        }

        if (connectionType == 2 && packet.type == PacketType.PLAYER_INFO) {
            relayRegisterConnection(packet);
            // Wait Certified
            connectionType = 3;
            ctx.writeAndFlush(relayServerInitInfoInternalPacket());
            sendVerifyClientValidity();
        }

        if (connectionType == 3 && packet.type == PacketType.RELAY_POW_RECEIVE) {
            if (receiveVerifyClientValidity(packet)) {
                // Certified End
                relayDirectInspection();
                //conn.startPingTask(); //心跳线程
                connectionType = 4;
            } else {
                sendVerifyClientValidity();
            }
        }

        if (connectionType == 4 && packet.type == PacketType.RELAY_118_117_RETURN) {
            serverQuestionReplyHandler(packet);
        }
        return true;
    }

    private void relayRegisterConnection(Packet packet) throws IOException {
        Packet sendPacket = packet;
        String registerPlayerId = conn.registerPlayerId;

        if (registerPlayerId == null || registerPlayerId.trim().isEmpty()) {
            try {
                GameInputStream stream = new GameInputStream(packet);
                stream.readString(); // server id
                stream.skip(12); // skip some bytes
                String name = stream.readString(); // player name
                stream.readIsString(); // read optional string
                stream.readString(); // read another string
                registerPlayerId = stream.readString(); // get player UUID hex
                conn.registerPlayerId = registerPlayerId;
                log.info("uuid+ "+registerPlayerId);
                conn.playerName = name;

                log.info(name);
            } catch (Exception e) {
                log.error("[No UUID-Hex]", e);
                return;
            }
        } else if (connectionType >= 5) { // PlayerPermission or higher

            //===========================================//
            //........关于ban等内容 恕不提供支持.........//
            //===========================================//
            if (connectionType == 5) { /*
                                        * // PlayerPermission
                                        * if (!currentRoom.isGaming()) {
                                        * // Check for duplicate UUID in room
                                        * for (RelayRoomConnection player : currentRoom.getConnections()) {
                                        * if (player.getConnectionType() == 6 && // PlayerJoinPermission
                                        * player.getRegisterPlayerId().equals(registerPlayerId)) {
                                        * // Kick player with duplicate UUID
                                        * kick("[UUID Check] HEX 重复, 换个房间试试");
                                        * return;
                                        * }
                                        * }
                                        * }
                                        * 
                                        * // Create player data if not exists
                                        * if (conn.getPlayerRelay() == null) {
                                        * PlayerRelay playerRelay =
                                        * currentRoom.getRelayPlayersData().get(registerPlayerId);
                                        * if (playerRelay == null) {
                                        * playerRelay = new PlayerRelay(conn, registerPlayerId, conn.getName());
                                        * currentRoom.getRelayPlayersData().put(registerPlayerId, playerRelay);
                                        * }
                                        * playerRelay.setNowName(conn.getName());
                                        * playerRelay.setDisconnect(false);
                                        * playerRelay.setConnection(conn);
                                        * conn.setPlayerRelay(playerRelay);
                                        * }
                                        * 
                                        * // Check bans
                                        * if (currentRoom.getRelayKickData().containsKey("BAN" + conn.getIp())) {
                                        * kick("[BAN] 您被这个房间BAN了 请换一个房间");
                                        * return;
                                        * }
                                        * 
                                        * // Check kicks
                                        * Integer time = currentRoom.getRelayKickData().get("KICK" + registerPlayerId);
                                        * if (time == null) {
                                        * time = currentRoom.getRelayKickData().get("KICK" + conn.getIpLong24());
                                        * }
                                        * 
                                        * if (time != null) {
                                        * if (time > System.currentTimeMillis() / 1000) {
                                        * kick("[踢出等待] 您被这个房间踢出了 请稍等一段时间 或者换一个房间");
                                        * return;
                                        * } else {
                                        * currentRoom.getRelayKickData().remove("KICK" + registerPlayerId);
                                        * currentRoom.getRelayKickData().remove("KICK" + conn.getIpLong24());
                                        * }
                                        * }
                                        * 
                                        * if (currentRoom.isGaming()) {
                                        * if (!currentRoom.isSyncFlag()) {
                                        * kick("[Sync Lock] 这个房间拒绝重连");
                                        * return;
                                        * }
                                        * 
                                        * // TODO: Implement sync count check
                                        * // This would require implementing PlayerSyncCount class
                                        * }
                                        */
            }

            // Handle player replacement in ongoing game
            if (conn.currentRoom.isGaming) {/*
                                               * if (!currentRoom.getReplacePlayerHex().isEmpty()) {
                                               * conn.setReplacePlayerHex(currentRoom.getReplacePlayerHex());
                                               * currentRoom.setReplacePlayerHex("");
                                               * currentRoom.sendMsg("玩家 " + conn.getName() + ", 取代了旧玩家");
                                               * }
                                               * if (!conn.getReplacePlayerHex().isEmpty()) {
                                               * GameOutputStream out = new GameOutputStream();
                                               * GameInputStream stream = new GameInputStream(packet);
                                               * out.writeString(stream.readString());
                                               * out.transferToFixedLength(stream, 12);
                                               * out.writeString(stream.readString());
                                               * out.writeIsString(stream);
                                               * out.writeString(stream.readString());
                                               * out.writeString(conn.getReplacePlayerHex());
                                               * stream.readString();
                                               * out.transferTo(stream);
                                               * sendPacket = out.createPacket(PacketType.PLAYER_INFO);
                                               * }
                                               */
            }

            connectionType = 6; // PlayerJoinPermission
            conn.currentRoom.sendPackageToHOST(sendPacket);
        }
    }

    // ============================================================================//
    // ..................................POW 验证................................. //
    // ============================================================================//
    private NetConnectProofOfWork netConnectAuthenticate;

    public void sendVerifyClientValidity() {
        netConnectAuthenticate = new NetConnectProofOfWork();
        int authenticateType = netConnectAuthenticate.getAuthenticateType();
        try {
            GameOutputStream o = new GameOutputStream();
            o.writeInt(netConnectAuthenticate.getResultInt());
            o.writeInt(authenticateType);

            if (authenticateType == 0 || (authenticateType >= 2 && authenticateType <= 4) || authenticateType == 6) {
                o.writeBoolean(true);
                o.writeInt(netConnectAuthenticate.getInitInt_1());
            } else {
                o.writeBoolean(false);
            }

            if (authenticateType == 1 || (authenticateType >= 2 && authenticateType <= 4)) {
                o.writeBoolean(true);
                o.writeInt(netConnectAuthenticate.getInitInt_2());
            } else {
                o.writeBoolean(false);
            }

            if (authenticateType >= 5 && authenticateType <= 6) {
                o.writeString(netConnectAuthenticate.getOutcome());
                o.writeString(netConnectAuthenticate.getFixedInitial());
                o.writeInt(netConnectAuthenticate.getMaximumNumberOfCalculations());
            }

            o.writeBoolean(false);
            ctx.writeAndFlush(o.createPacket(PacketType.RELAY_POW));
        } catch (Exception e) {
        }
    }

    public boolean receiveVerifyClientValidity(Packet packet) throws IOException {
        GameInputStream inStream = new GameInputStream(packet);
        if (netConnectAuthenticate != null) {
            if (netConnectAuthenticate.verifyPOWResult(
                    inStream.readInt(),
                    inStream.readInt(),
                    inStream.readString())) {
                netConnectAuthenticate = null;
                return true;
            }
        } else {
            // Ignore, Under normal circumstances, it should not reach here,
            // and the processor will handle it
        }
        // Ignore, There should be no errors in this part,
        // errors will only come from constructing false error packets
        return false;
    }

    // ============================================================================//
    // .................................创建与连接................................ //
    // ============================================================================//

    private static Packet relayServerInitInfoInternalPacket() throws IOException {
        GameOutputStream o = new GameOutputStream();
        o.writeByte(0);
        // RELAY Version
        o.writeInt(151);
        // ?
        o.writeInt(1);
        // ?
        o.writeBoolean(false);
        return o.createPacket(PacketType.RELAY_VERSION_INFO);
    }

    private static boolean isBlank(Object string) {
        return string == null || "".equals(string.toString().trim());
    }

    public String relaySelect;

    public void relayDirectInspection() throws IOException {
        GameInputStream inStream = new GameInputStream(conn.cachePacket);
        inStream.readString();
        int packetVersion = inStream.readInt();
        clientVersion = inStream.readInt();
        // betaGameVersion = getBetaVersion(clientVersion);

        if (packetVersion >= 1) {
            inStream.skip(4);
        }
        String queryString = ""; // queryString的意思： micro.xin:5123/new 此时请求的new部分会被写入queryString
        if (packetVersion >= 2) {
            queryString = inStream.readIsString();
        }
        if (packetVersion >= 3) {
            // Player Name
            inStream.readString();
        }

        log.info("ques {}", queryString);
        if (queryString == null || queryString.isEmpty()
                || !RelayRoomManager.containsRoom(Integer.parseInt(queryString))) {
            // 如果不存在那就要创建
            if (isBlank(queryString) || "RELAYCN".equalsIgnoreCase(queryString)) {
                ctx.writeAndFlush(
                        UniversalPacket.packetQuestion(5, "[Relay CN+ #0] 这台服务器是CN非官方的Relay房间\n您输入的房间不存在,不过您也可以输入new来创建一个"));// Data.SERVER_CORE_VERSION
                // relaySelect = "3.0.0";
            } else {
                idCustom(queryString);
            }
        } else {
            // this.room = relayRoom;
            addRelayConnect(RelayRoomManager.getRoom(Integer.parseInt(queryString)));
        }
    }

    public void serverQuestionReplyHandler(Packet packet) {
        try {

            GameInputStream inStream = new GameInputStream(packet);
            // Skip the previously useless data
            inStream.skip(5);
            // Read data and remove leading and trailing spaces
            String id = inStream.readString().trim();
            log.info("debug Question Responed {}", id);
            idCustom(id);
        } catch (Exception e) {
        }
    }

    private void idCustom(String inId) throws IOException {
        // 过滤制表符、空格、换行符
        String id = inId.replaceAll("\\s", "");

        // 处理"old"关键字
        // if ("old".equalsIgnoreCase(id)) {
        // id = RelayRoom.serverRelayOld.getOrDefault(registerPlayerId, "");
        // } else {
        // RelayRoom.serverRelayOld.put(registerPlayerId, id);
        // }

        if (id.isEmpty()) {
            ctx.writeAndFlush(UniversalPacket.packetQuestion(5, "[提示] 请输入房间ID或'new'创建新房间"));
            return;
        }

        // 检查Emoji
        if (containsEmoji(id)) {
            ctx.writeAndFlush(UniversalPacket.packetQuestion(5, "[错误] 不能使用Emoji"));
            return;
        }

        // 新建房间逻辑
        if (id.equalsIgnoreCase("new")) {
            createNewRoom();
            return;
        }

        // 加入现有房间逻辑
        try {
            if (id.contains(".")) {
                ctx.writeAndFlush(UniversalPacket.packetQuestion(5, "[错误] ID不能包含点号(.)"));
                return;
            }

            if (RelayRoomManager.containsRoom(Integer.parseInt(id))) {
                addRelayConnect(RelayRoomManager.getRoom(Integer.parseInt(id)));
            } else {
                ctx.writeAndFlush(UniversalPacket.packetQuestion(5, "[错误] 找不到房间: " + id));
            }
        } catch (Exception e) {
            log.debug("Error finding relay room", e);
            ctx.writeAndFlush(UniversalPacket.packetQuestion(5, "[错误] " + e.getMessage()));
        }
    }

    private void addRelayConnect(RelayNetworkRoom room) throws IOException {
        connectionType = 5;
        conn.currentRoom = room;

        // connectReceiveData.inputPassword = false
        // if (room == null) {
        // Log.clog("?????")
        // room = NetStaticData.relayRoom
        // }

        conn.currentRoom.site++;
        site = conn.currentRoom.site;
        // room!!.setAbstractNetConnect(this)
        conn.currentRoom.connections[conn.currentRoom.site] = this.conn;

        GameOutputStream o = new GameOutputStream();
        if (clientVersion >= 172) {
            o.writeByte(1);
            o.writeInt(site);
            // ?
            o.writeString(conn.registerPlayerId);
            // o.writeBoolean(false)
            // User UUID
            o.writeIsString(null);
            o.writeIsString("10.0.0.1");
            // 不用SendtoHost 因为SendToHost会对包进行一些处理
            conn.currentRoom.adminConn.handler.ctx.writeAndFlush(o.createPacket(PacketType.FORWARD_CLIENT_ADD));
        } else {
            o.writeByte(0);
            o.writeInt(site);
            o.writeString(conn.registerPlayerId);
            o.writeIsString(null);
            conn.currentRoom.adminConn.handler.ctx.writeAndFlush(o.createPacket(PacketType.FORWARD_CLIENT_ADD));
        }

        conn.currentRoom.sendPackageToHOST(conn.cachePacket);
        // connectionAgreement.add(room!!.groupNet);
        // this.room!!.setAddSize()
        // currentRoom.site++;
    }

    // 创建新房间
    private void createNewRoom() throws IOException {
        try {
            // 由服务器自动生成房间ID
            int tmpId = ThreadLocalRandom.current().nextInt(10000, 100000);
            if (!RelayRoomManager.containsRoom(tmpId)) {
                RelayNetworkRoom NewRoom = new RelayNetworkRoom(tmpId, this.conn);
                RelayRoomManager.addRelayRoom(NewRoom);
                conn.currentRoom = NewRoom;
            }else{
                ctx.writeAndFlush(UniversalPacket.packetQuestion(5, "[?] 随机的ID居然已经存在了 0.01%的概率哎?!"));
            }

            // 发送成功消息
            ctx.writeAndFlush(UniversalPacket.packetQuestion(5, "[成功] 已创建新房间，ID: " + conn.currentRoom.roomId));

            // 设置为房主
            sendRelayServerId();

            // 设置默认参数
            // sendDefaultRoomSettings();

        } catch (Exception e) {
            log.error("Failed to create new room", e);
            ctx.writeAndFlush(UniversalPacket.packetQuestion(5, "[错误] 创建房间失败: " + e.getMessage()));
        }
    }

    private boolean containsEmoji(String input) {
        return input.matches(".*[\\p{So}].*");
    }

    public void sendRelayServerId() throws IOException {
        // 确保连接已准备好
        // connectReceiveData.setInputPassword(false);

        if (conn.currentRoom == null) {
            log.info("sendRelayServerId -> relay : null");
            // currentRoom = NetStaticData.getRelayRoom();
        }

        // 如果已有位置(site != -1)，先移除旧连接
        // if (site != -1) {
        // currentRoom.removeAbstractNetConnect(site);
        // // -2 表示这是第二任房主
        // site = -2;
        // }

        // 设置当前连接为房主
        conn.currentRoom.adminConn = this.conn;
        host = true;
        connectionType = 7; // HostPermission

        boolean isPublic = false; // 默认房间不公开
        GameOutputStream o = new GameOutputStream();

        if (clientVersion >= 172) {
            // 新协议版本(172+)的数据包格式
            o.writeByte(2);
            o.writeBoolean(true); // allowThisConnectionForwarding
            o.writeBoolean(true); // removeThisConnection
            o.writeBoolean(true); // ?
            o.writeString(SERVER_RELAY_UUID);
            o.writeBoolean(false); // MOD标记
            o.writeBoolean(isPublic); // 是否公开
            o.writeBoolean(true); // ?
            o.writeString(
                    "{{CORE-CN}} Room ID : "+conn.currentRoom.roomId+" \n" +
                            "你的房间是 <" + (isPublic ? "开放" : "隐藏") + "> 在列表\n" +
                            "本服务端为Micro维护的第三方Rukkit版本\n" +
                            "This Server Use RukkitNEXT Project (it)");
            o.writeBoolean(multicast); // 是否使用组播
            o.writeIsString(conn.registerPlayerId); // 注册玩家ID
            log.info("uuid+ "+conn.registerPlayerId);
        } else {
            // 旧协议版本的数据包格式
            o.writeByte(1);
            o.writeBoolean(true); // allowThisConnectionForwarding
            o.writeBoolean(true); // removeThisConnection
            o.writeIsString(SERVER_RELAY_UUID); // 服务器UUID
            o.writeBoolean(false); // MOD标记
            o.writeBoolean(isPublic); // 是否公开
            o.writeString(
                    "{{CORE-CN}} Room ID : "+conn.currentRoom.roomId+" \n" +
                            "你的房间是 <" + (isPublic ? "开放" : "隐藏") + "> 在列表\n" +
                            "本服务端为Micro维护的第三方Rukkit版本\n" +
                            "This Server Use RukkitNEXT Project (it)");
            o.writeBoolean(multicast); // 是否使用组播
        }

        // 发送RELAY_BECOME_SERVER数据包
        ctx.writeAndFlush(o.createPacket(PacketType.RELAY_BECOME_SERVER));
        // ctx.writeAndFlush(o.createPacket(PacketType.RELAY_BECOME_SERVER));

        // 禁止玩家使用 Server/Relay 做玩家名
        if (conn.playerName.equalsIgnoreCase("SERVER") || conn.playerName.equalsIgnoreCase("RELAY")) {
            // currentRoom.closeRoom(); // 关闭房间
            log.error("有人使用了SEVR作为玩家名字");
        }
    }
}
