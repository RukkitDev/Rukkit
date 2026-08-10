/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.core.handler;

import cn.rukkit.Rukkit;
import cn.rukkit.event.player.PlayerJoinEvent;
import cn.rukkit.event.player.PlayerReconnectEvent;
import cn.rukkit.game.NetworkPlayer;
import cn.rukkit.network.ConnectionState;
import cn.rukkit.network.core.packet.Packet;
import cn.rukkit.network.core.packet.PacketType;
import cn.rukkit.network.core.packet.UniversalPacket;
import cn.rukkit.network.io.GameInputStream;
import cn.rukkit.network.room.ServerGlobalConnectionManager;
import cn.rukkit.network.room.ServerRoom;
import cn.rukkit.network.room.ServerRoomConnection;
import cn.rukkit.network.room.ServerRoomManager;
import cn.rukkit.util.LangUtil;

import java.util.List;
import java.util.Objects;

/** Master-compatible player registration handler for the core packet stack. */
public class ServerPlayerInfoHandler extends ServerPacketHandler {
    private final ServerRoomManager roomManager;
    private final ServerGlobalConnectionManager globalConnectionManager;

    public ServerPlayerInfoHandler(ServerRoomManager roomManager,
                                   ServerGlobalConnectionManager globalConnectionManager) {
        this.roomManager = Objects.requireNonNull(roomManager, "roomManager must not be null");
        this.globalConnectionManager = Objects.requireNonNull(
                globalConnectionManager, "globalConnectionManager must not be null");
    }

    @Override
    public int getType() {
        return PacketType.PLAYER_INFO;
    }

    @Override
    public List<ConnectionState> getAllowedStates() {
        return List.of(ConnectionState.PRE_REGISTERED);
    }

    @Override
    public void handle(ServerPacketContext context, Packet packet) throws Exception {
        GameInputStream input = new GameInputStream(packet);
        String packageName = input.readString();
        getLogger().debug("Ints:{}", input.readInt());
        int gameVersionCode = input.readInt();
        input.readInt();
        String playerName = input.readString();
        input.readByte();
        input.readString();
        String uuid = input.readString();
        int coreUnitCheck = input.readInt();
        input.readString();
        getLogger().debug("Got Player(package={}, version={}, name={}, uuid={}, coreUnit={})",
                packageName, gameVersionCode, playerName, uuid, coreUnitCheck);

        synchronized (roomManager) {
            ServerRoom room = roomManager.getAvailableRoom();
            NetworkPlayer targetPlayer = globalConnectionManager.getAllPlayerByUUID(uuid);
            ServerRoom currentRoom;
            if (targetPlayer != null && Rukkit.getConfig().syncEnabled
                    && targetPlayer.getServerRoom() != null) {
                currentRoom = targetPlayer.getServerRoom();
                getLogger().info("Found offline room {}", currentRoom);
            } else {
                currentRoom = room;
            }
            context.handler().setCurrentRoom(currentRoom);

            if (currentRoom == null) {
                context.ctx().writeAndFlush(UniversalPacket.kick(LangUtil.getString("rukkit.gameFull")));
                return;
            }

            synchronized (currentRoom) {
                boolean reconnecting = targetPlayer != null && Rukkit.getConfig().syncEnabled;
                if (!currentRoom.isGaming() && targetPlayer != null) {
                    getLogger().info("Dup player {} (UUID={}) joined!", playerName, uuid);
                    if (Rukkit.getConfig().isDebug) {
                        getLogger().info("You are in the debug mode, allowing this situation!");
                        targetPlayer = null;
                        reconnecting = false;
                    } else {
                        context.ctx().writeAndFlush(UniversalPacket.kick("You are already in server!"));
                        return;
                    }
                }

                if (currentRoom.isGaming() && !reconnecting) {
                    context.ctx().writeAndFlush(
                            UniversalPacket.kick(LangUtil.getString("rukkit.gameStarted")));
                    return;
                }

                ServerRoomConnection connection = new ServerRoomConnection(context.handler(), currentRoom);
                if (reconnecting) {
                    connection.player = targetPlayer;
                    connection.player.name = playerName;
                    connection.player.bindServerConnection(connection);
                } else {
                    NetworkPlayer player = new NetworkPlayer(connection);
                    player.name = playerName;
                    player.uuid = uuid;
                    connection.player = player;
                }
                context.bindConnection(connection);

                boolean registered = reconnecting
                        ? currentRoom.connectionManager.set(connection, connection.player.playerIndex)
                        : currentRoom.connectionManager.add(connection);
                if (!registered) {
                    context.bindConnection(null);
                    context.ctx().writeAndFlush(
                            UniversalPacket.kick(LangUtil.getString("rukkit.gameFull")));
                    return;
                }
                globalConnectionManager.add(connection);

                context.ctx().writeAndFlush(UniversalPacket.serverInfo(currentRoom.config));
                if (currentRoom.connectionManager.size() <= 1) {
                    connection.sendServerMessage(LangUtil.getString("rukkit.playerGotAdmin"));
                    connection.player.isAdmin = true;
                    context.ctx().writeAndFlush(UniversalPacket.serverInfo(currentRoom.config, true));
                }

                if (currentRoom.isGaming()) {
                    getLogger().info("Start Syncing!");
                    context.handler().stopTimeout();
                    connection.player.updateServerInfo();
                    connection.startTeamTask();
                    connection.updateTeamList(false);
                    connection.startPingTask();
                    connection.handler.ctx.writeAndFlush(
                            UniversalPacket.startGame(connection.currectRoom.config));
                    currentRoom.syncGame();
                    connection.player.isDisconnected = false;
                    PlayerReconnectEvent.getListenerList().callListeners(
                            new PlayerReconnectEvent(connection.player));
                }

                try {
                    connection.player.loadPlayerData();
                } catch (Exception e) {
                    getLogger().warn("Player {} data load failed!", playerName, e);
                }
                String simpleUuid = uuid.length() > 7 ? uuid.substring(0, 7) : uuid;
                connection.sendServerMessage(LangUtil.getFormatString("rukkit.room", currentRoom.roomId));
                connection.sendServerMessage(Rukkit.getConfig().welcomeMsg
                        .replace("{playerName}", playerName)
                        .replace("{simpleUUID}", simpleUuid)
                        .replace("{packageName}", packageName)
                        .replace("{versionCode}", String.valueOf(gameVersionCode)));

                if (!reconnecting) {
                    connection.startPingTask();
                    connection.startTeamTask();
                    connection.updateTeamList(false);
                    context.handler().stopTimeout();
                    PlayerJoinEvent.getListenerList().callListeners(
                            new PlayerJoinEvent(connection.player));
                }

                if (currentRoom.isGaming()) {
                    context.transitionTo(ConnectionState.IN_GAME);
                } else {
                    context.transitionTo(ConnectionState.IN_ROOM);
                }
            }
        }
    }
}
