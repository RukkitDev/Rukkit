/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.packet.handler;

import cn.rukkit.Rukkit;
import cn.rukkit.network.ConnectionState;
import cn.rukkit.network.GameInputStream;
import cn.rukkit.network.NetworkRoom;
import cn.rukkit.network.RoomConnection;
import cn.rukkit.network.packet.Packet;
import cn.rukkit.event.player.PlayerJoinEvent;
import cn.rukkit.event.player.PlayerReconnectEvent;
import cn.rukkit.game.NetworkPlayer;
import cn.rukkit.util.LangUtil;

import java.util.List;

public class PlayerInfoHandler extends PacketHandler {
    @Override
    public int getType() {
        return Packet.PACKET_PLAYER_INFO;
    }

    @Override
    public List<ConnectionState> getAllowedStates() {
        return List.of(ConnectionState.PRE_REGISTERED);
    }

    @Override
    public void handle(PacketContext ctx, Packet packet) throws Exception {
        GameInputStream in = new GameInputStream(packet);
        String packageName = in.readString();
        getLogger().debug("Ints:{}", in.readInt());
        int gameVersionCode = in.readInt();
        in.readInt();
        String playerName = in.readString();
        in.readByte();
        in.readString();
        String uuid = in.readString();
        int coreUnitCheck = in.readInt();
        in.readString();
        getLogger().debug("Got Player(package={}, version={}, name={}, uuid={}, coreUnit={})",
                packageName, gameVersionCode, playerName, uuid, coreUnitCheck);

        NetworkRoom room = Rukkit.getRoomManager().getAvailableRoom();
        NetworkPlayer targetPlayer = Rukkit.getGlobalConnectionManager().getAllPlayerByUUID(uuid);
        NetworkRoom currentRoom;
        if (targetPlayer != null && Rukkit.getConfig().syncEnabled) {
            currentRoom = targetPlayer.getRoom();
            getLogger().info("Found offline room {}", currentRoom);
        } else {
            currentRoom = room;
        }
        ctx.handler().setCurrentRoom(currentRoom);

        if (currentRoom == null) {
            ctx.ctx().writeAndFlush(Packet.kick(LangUtil.getString("rukkit.gameFull")));
            return;
        }

        if (!currentRoom.isGaming() && targetPlayer != null) {
            getLogger().info("Dup player {} (UUID={}) joined!", playerName, uuid);
            if (Rukkit.getConfig().isDebug) {
                getLogger().info("You are in the debug mode, allowing this situation!");
                targetPlayer = null;
            } else {
                ctx.ctx().writeAndFlush(Packet.kick("You are already in server!"));
                return;
            }
        }

        ctx.ctx().writeAndFlush(Packet.serverInfo(currentRoom.config));

        RoomConnection connection = new RoomConnection(ctx.handler(), currentRoom);
        if (targetPlayer != null && Rukkit.getConfig().syncEnabled) {
            connection.player = targetPlayer;
            connection.player.name = playerName;
        } else {
            NetworkPlayer player = new NetworkPlayer(connection);
            player.name = playerName;
            player.uuid = uuid;
            connection.player = player;
        }
        ctx.bindConnection(connection);

        if (currentRoom.connectionManager.size() <= 0) {
            connection.sendServerMessage(LangUtil.getString("rukkit.playerGotAdmin"));
            connection.player.isAdmin = true;
            ctx.ctx().writeAndFlush(Packet.serverInfo(currentRoom.config, true));
        } else {
            ctx.ctx().writeAndFlush(Packet.serverInfo(currentRoom.config));
        }

        if (currentRoom.isGaming()) {
            if (Rukkit.getConfig().syncEnabled) {
                getLogger().info("Start Syncing!");
                ctx.handler().stopTimeout();
                connection.player.updateServerInfo();
                currentRoom.connectionManager.set(connection, connection.player.playerIndex);
                connection.startTeamTask();
                connection.updateTeamList(false);
                connection.startPingTask();
                connection.handler.ctx.writeAndFlush(Packet.startGame());
                currentRoom.syncGame();
                connection.player.isDisconnected = false;
                PlayerReconnectEvent.getListenerList().callListeners(new PlayerReconnectEvent(connection.player));
            } else {
                ctx.ctx().writeAndFlush(Packet.kick(LangUtil.getString("rukkit.gameStarted")));
                return;
            }
        }

        Rukkit.getGlobalConnectionManager().add(connection);
        if (targetPlayer == null) {
            currentRoom.connectionManager.add(connection);
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

        if (targetPlayer == null) {
            connection.startPingTask();
            connection.startTeamTask();
            connection.updateTeamList(false);
            ctx.handler().stopTimeout();
            PlayerJoinEvent.getListenerList().callListeners(new PlayerJoinEvent(connection.player));
        }

        if (currentRoom.isGaming()) {
            ctx.transitionTo(ConnectionState.IN_GAME);
        } else {
            ctx.transitionTo(ConnectionState.IN_ROOM);
        }
    }
}
