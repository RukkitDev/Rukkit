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
import cn.rukkit.command.ChatCommandContext;
import cn.rukkit.command.ChatCommandContextListener;
import cn.rukkit.command.CommandManager;
import cn.rukkit.command.RoomCommandContext;
import cn.rukkit.event.EventHandler;
import cn.rukkit.event.EventListener;
import cn.rukkit.event.player.PlayerChatEvent;
import cn.rukkit.game.NetworkPlayer;
import cn.rukkit.game.PingType;
import cn.rukkit.game.PlayerManager;
import cn.rukkit.game.map.CustomMapLoader;
import cn.rukkit.game.map.OfficialMap;
import cn.rukkit.network.core.packet.UniversalPacket;
import cn.rukkit.network.room.ServerRoomConnection;
import cn.rukkit.plugin.PluginConfig;
import cn.rukkit.util.LangUtil;
import cn.rukkit.util.VersionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

/**
 * Chat commands owned by the migrated network stack.
 *
 * <p>This plugin intentionally has no legacy {@code RoomConnection} listener.
 * The old command implementation remains in {@link CommandPlugin}; keeping
 * the registrations separate makes it impossible for core startup to
 * accidentally dispatch a command through the old connection model.</p>
 */
public class CoreCommandPlugin extends InternalRukkitPlugin
        implements ChatCommandContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(CoreCommandPlugin.class);
    private int totalInfo;

    /** Stops an in-progress AFK vote when the administrator chats. */
    public class CommandEventListener implements EventListener {
        @EventHandler
        public void playerChat(PlayerChatEvent event) {
            NetworkPlayer player = event.getPlayer();
            if (player.getServerRoom() == null || !player.isAdmin) {
                return;
            }
            if ("afk".equals(player.getServerRoom().vote.voteId)) {
                player.getServerRoom().connectionManager
                        .broadcastServerMessage("Countdown stopped!");
                player.getServerRoom().vote.stopVote();
            }
        }
    }

    @Override
    public boolean onSend(ChatCommandContext context, String[] args) {
        context.sendServerMessage(versionMessage());
        return false;
    }

    private static String versionMessage() {
        return "Rukkit Server v" + VersionUtil.getVersion() + "\n"
                + "Rukkit Plugin API v" + Rukkit.PLUGIN_API_VERSION;
    }

    @Override
    public void loadConfig() {
        config = new PluginConfig();
        config.name = "Core Chat Command Plugin";
        config.author = "rukkit";
        config.version = VersionUtil.getVersion();
        config.id = "core-command-plugin";
        config.pluginClass = "cn.rukkit.plugin.internal.CoreCommandPlugin";
        config.apiVersion = Rukkit.PLUGIN_API_VERSION;
    }

    private static RoomCommandContext roomContext(ChatCommandContext context) {
        return context instanceof RoomCommandContext room ? room : null;
    }

    private static void broadcastServerInfo(RoomCommandContext context) {
        try {
            context.room().connectionManager.broadcast(
                    UniversalPacket.serverInfo(context.room().config));
            context.connection().sendPacket(
                    UniversalPacket.serverInfo(context.room().config, true));
        } catch (IOException ignored) {
            // Keep command dispatch alive if a client channel closes mid-command.
        }
    }

    private void register(CommandManager manager, String command, String help,
                          int args, boolean adminRequired,
                          ChatCommandContextListener listener) {
        manager.registerCommand(ChatCommand.contextCommand(command, help, args,
                listener, this, adminRequired));
    }

    public static class VersionCallback implements ChatCommandContextListener {
        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            context.sendServerMessage(versionMessage());
            return false;
        }
    }

    public static class KickCallback implements ChatCommandContextListener {
        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            RoomCommandContext room = roomContext(context);
            if (room == null || args.length < 1) {
                return true;
            }
            try {
                NetworkPlayer player = room.room().playerManager
                        .get(Integer.parseInt(args[0]));
                if (player == null || player.isEmpty || player.getServerConnection() == null) {
                    context.sendServerMessage(LangUtil.getString("chat.playerEmpty"));
                } else {
                    player.getServerConnection().kick(LangUtil.getString("chat.kicked"));
                }
            } catch (NumberFormatException e) {
                context.sendServerMessage(LangUtil.getString("chat.playerEmpty"));
            }
            return true;
        }
    }

    public static class TeamChatCallback implements ChatCommandContextListener {
        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            if (args.length > 0) {
                context.player().sendTeamMessage(args[0]);
            }
            return false;
        }
    }

    public static class MapsCallback implements ChatCommandContextListener {
        private final int type;

        public MapsCallback(int type) {
            this.type = type;
        }

        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            RoomCommandContext room = roomContext(context);
            if (room == null) {
                return false;
            }
            if (type == 0) {
                StringBuilder build = new StringBuilder();
                int page = MapCommandSupport.pageIndex(args);
                if (page < 0) {
                    return false;
                }
                if (args.length > 0) {
                    build.append("- Maps -  Page ").append(args[0]).append(" \n");
                    for (int i = page * MapCommandSupport.PAGE_SIZE;
                         i < MapCommandSupport.pageEnd(page, OfficialMap.maps.length); i++) {
                        build.append(String.format("[%d] %s", i, OfficialMap.maps[i]))
                                .append("\n");
                    }
                } else {
                    build.append("- Help -  Page 1 \n");
                    for (int i = 0; i < 10 && i < OfficialMap.maps.length; i++) {
                        build.append(String.format("[%d] %s", i, OfficialMap.maps[i]))
                                .append("\n");
                    }
                }
                context.sendServerMessage(build.toString());
                return false;
            }

            if (!context.player().isAdmin || args.length < 1) {
                return false;
            }
            String mapName = null;
            String mapString = MapCommandSupport.quotedValue(args[0]);
            int mapIndex = mapString == null
                    ? MapCommandSupport.mapIndex(args[0], OfficialMap.maps.length)
                    : MapCommandSupport.officialMapIndex(mapString);
            if (mapIndex >= 0) {
                mapName = OfficialMap.maps[mapIndex];
            } else {
                return false;
            }
            if (mapName != null) {
                room.room().config.mapName = mapName;
                room.room().config.mapType = 0;
                broadcastServerInfo(room);
            }
            return false;
        }
    }

    public static class CustomMapsCallback implements ChatCommandContextListener {
        private final int type;

        public CustomMapsCallback(int type) {
            this.type = type;
        }

        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            RoomCommandContext room = roomContext(context);
            if (room == null) {
                return false;
            }
            List<String> maps = CustomMapLoader.getMapNameList();
            if (type == 0) {
                StringBuilder build = new StringBuilder();
                int page = MapCommandSupport.pageIndex(args);
                if (page < 0) {
                    return false;
                }
                if (args.length > 0) {
                    build.append("- CustomMaps -  Page ").append(args[0]).append(" \n");
                    for (int i = page * MapCommandSupport.PAGE_SIZE;
                         i < MapCommandSupport.pageEnd(page, maps.size()); i++) {
                        build.append(String.format("[%d] %s", i, maps.get(i)))
                                .append("\n");
                    }
                } else {
                    build.append("- Help -  Page 1 \n");
                    for (int i = 0; i < Math.min(maps.size(), 10); i++) {
                        build.append(String.format("[%d] %s", i, maps.get(i)))
                                .append("\n");
                    }
                }
                context.sendServerMessage(build.toString());
                return false;
            }

            if (context.player().isAdmin && args.length > 0) {
                int mapIndex = MapCommandSupport.mapIndex(args[0], maps.size());
                if (mapIndex < 0) {
                    return false;
                }
                room.room().config.mapName = maps.get(mapIndex);
                room.room().config.mapType = 1;
                broadcastServerInfo(room);
            }
            return false;
        }
    }

    class MoveCallback implements ChatCommandContextListener {
        private final int type;

        MoveCallback(int type) {
            this.type = type;
        }

        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            RoomCommandContext room = roomContext(context);
            if (room == null) {
                return false;
            }
            if (type == 0) {
                movePlayer(room, context, args);
            } else if (type == 1) {
                moveSelf(room, context, args);
            }
            return false;
        }

        private void movePlayer(RoomCommandContext room, ChatCommandContext context,
                                String[] args) {
            if (!context.player().isAdmin || room.room().isGaming() || args.length < 2) {
                return;
            }
            try {
                PlayerManager players = room.room().playerManager;
                NetworkPlayer from = players.get(Integer.parseInt(args[0]) - 1);
                NetworkPlayer target = players.get(Integer.parseInt(args[1]) - 1);
                if (from == null || target == null || from.isEmpty || target.isEmpty) {
                    context.sendServerMessage(LangUtil.getString("chat.playerEmpty"));
                    return;
                }
                if (args.length == 3) {
                    int team = Integer.parseInt(args[2]);
                    from.team = (team == -1 || team == -2)
                            ? (target.playerIndex % 2 == 1 ? 1 : 0) : team;
                }
                if (from.movePlayer(Integer.parseInt(args[1]) - 1)) {
                    context.sendServerMessage(LangUtil.getString("chat.moveComplete"));
                    return;
                }
                int fromSlot = from.playerIndex;
                int toSlot = target.playerIndex;
                if (fromSlot == toSlot) {
                    context.sendServerMessage("not same player!");
                    return;
                }
                players.remove(target);
                from.movePlayer(toSlot);
                target.movePlayer(fromSlot);
            } catch (RuntimeException e) {
                LOG.error("Error moving player", e);
            }
        }

        private void moveSelf(RoomCommandContext room, ChatCommandContext context,
                              String[] args) {
            if (room.room().isGaming() || args.length < 1) {
                return;
            }
            try {
                if (args.length == 2) {
                    int team = Integer.parseInt(args[1]);
                    context.player().team = (team == -1 || team == -2)
                            ? ((Integer.parseInt(args[0]) - 1) % 2 == 1 ? 1 : 0)
                            : team;
                }
                if (context.player().movePlayer(Integer.parseInt(args[0]) - 1)) {
                    context.sendServerMessage(LangUtil.getString("chat.moveComplete"));
                } else {
                    context.sendServerMessage(LangUtil.getString("chat.playerExist"));
                }
            } catch (RuntimeException e) {
                LOG.error("Error moving player", e);
            }
        }
    }

    class QcCallback implements ChatCommandContextListener {
        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            if (!(context instanceof RoomCommandContext room) || args.length == 0) {
                return false;
            }
            getLogger().info("Player {} issued command: {}", context.player().name, args[0]);
            String nested = CommandManager.normalizeNestedCommand(args[0]);
            Rukkit.getCommandManager().executeChatCommand(
                    room.connection(), Rukkit.getCoreGlobalConnectionManager(), nested);
            return false;
        }
    }

    class TeamCallback implements ChatCommandContextListener {
        private final int type;

        TeamCallback(int type) {
            this.type = type;
        }

        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            RoomCommandContext room = roomContext(context);
            if (room == null || room.room().isGaming()
                    || args.length < (type == 0 ? 2 : 1)) {
                return false;
            }
            try {
                if (type == 0) {
                    if (!context.player().isAdmin) {
                        return false;
                    }
                    int team = Integer.parseInt(args[1]) - 1;
                    int slot = Integer.parseInt(args[0]) - 1;
                    NetworkPlayer target = room.room().playerManager.get(slot);
                    if (target == null || target.isEmpty) {
                        context.sendServerMessage(LangUtil.getString("chat.playerEmpty"));
                        return false;
                    }
                    if (team == -1 || team == -2) {
                        target.team = slot % 2 == 1 ? 1 : 2;
                    }
                    target.team = team;
                } else {
                    context.player().team = Integer.parseInt(args[0]) - 1;
                }
            } catch (RuntimeException e) {
                context.sendServerMessage(LangUtil.getString("chat.playerEmpty"));
            }
            return false;
        }
    }

    static class HelpCallback implements ChatCommandContextListener {
        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            context.sendServerMessage(buildHelpMessage(args));
            return false;
        }

        private static String buildHelpMessage(String[] args) {
            StringBuilder build = new StringBuilder();
            if (args.length > 0) {
                build.append("- Help -  Page ").append(args[0]).append(" \n");
                int page = Integer.parseInt(args[0]) - 1;
                int index = 0;
                for (ChatCommand command : Rukkit.getCommandManager().getLoadedCommand().values()) {
                    if (index++ < page * 10) {
                        continue;
                    }
                    if (index > page * 10 + 10) {
                        break;
                    }
                    build.append(String.format("%s : %s", command.cmd, command.helpMessage))
                            .append("\n");
                }
            } else {
                build.append("- Help -  Page 1 \n");
                int index = 0;
                for (ChatCommand command : Rukkit.getCommandManager().getLoadedCommand().values()) {
                    if (index++ > 10) {
                        break;
                    }
                    build.append(String.format("%s : %s", command.cmd, command.helpMessage))
                            .append("\n");
                }
            }
            return build.toString();
        }
    }

    class InfoCallback implements ChatCommandContextListener {
        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            totalInfo++;
            LOG.warn("{} send a info: {}", context.player().name,
                    args.length == 0 ? "" : args[0]);
            return false;
        }
    }

    class StartCallback implements ChatCommandContextListener {
        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            RoomCommandContext room = roomContext(context);
            if (room == null) {
                return true;
            }
            if (!room.room().isGaming() && context.player().isAdmin) {
                if (room.room().connectionManager.size() < Rukkit.getConfig().minStartPlayer) {
                    room.room().connectionManager.broadcastServerMessage(
                            MessageFormat.format(LangUtil.getString("chat.minStartPlayer"),
                                    Rukkit.getConfig().minStartPlayer));
                } else {
                    room.room().startGame();
                }
            }
            return true;
        }
    }

    class SetFogCallback implements ChatCommandContextListener {
        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            RoomCommandContext room = roomContext(context);
            if (room == null || room.room().isGaming() || !context.player().isAdmin
                    || args.length < 1) {
                return false;
            }
            switch (args[0]) {
                case "off" -> room.room().config.fogType = 0;
                case "basic" -> room.room().config.fogType = 1;
                case "los" -> room.room().config.fogType = 2;
                default -> room.room().config.fogType = 2;
            }
            broadcastServerInfo(room);
            return false;
        }
    }

    class StartingUnitCallback implements ChatCommandContextListener {
        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            RoomCommandContext room = roomContext(context);
            if (room == null || room.room().isGaming() || !context.player().isAdmin
                    || args.length < 1) {
                return false;
            }
            room.room().config.startingUnits = Integer.parseInt(args[0]);
            broadcastServerInfo(room);
            return false;
        }
    }

    class ShareCallback implements ChatCommandContextListener {
        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            RoomCommandContext room = roomContext(context);
            if (room == null || context.player() == null) {
                return false;
            }
            if (!room.room().config.sharedControl) {
                context.sendServerMessage("[Shared control is not enabled in this game]");
                return false;
            }

            String value = args != null && args.length > 0 ? args[0] : "";
            if ("true".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value)) {
                if (!context.player().isSharingControl) {
                    context.player().isSharingControl = true;
                    room.room().connectionManager.broadcastServerMessage(
                            "[shared control now on for " + context.player().name + "]");
                } else {
                    room.room().connectionManager.broadcastServerMessage(
                            "[shared control already on for " + context.player().name + "]");
                }
                return false;
            }
            if ("false".equalsIgnoreCase(value) || "off".equalsIgnoreCase(value)) {
                if (context.player().isSharingControl) {
                    context.player().isSharingControl = false;
                    room.room().connectionManager.broadcastServerMessage(
                            "[shared control now off for " + context.player().name + "]");
                } else {
                    room.room().connectionManager.broadcastServerMessage(
                            "[shared control already off for " + context.player().name + "]");
                }
                return false;
            }
            context.sendServerMessage("[Expected true or false]");
            return false;
        }
    }

    class SharedControlCallback implements ChatCommandContextListener {
        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            RoomCommandContext room = roomContext(context);
            if (room == null || room.room().isGaming() || !context.player().isAdmin
                    || args.length < 1) {
                return false;
            }
            room.room().config.sharedControl = Boolean.parseBoolean(args[0]);
            broadcastServerInfo(room);
            return false;
        }
    }

    class NukeCallback implements ChatCommandContextListener {
        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            RoomCommandContext room = roomContext(context);
            if (room == null || room.room().isGaming() || !context.player().isAdmin
                    || args.length < 1) {
                return false;
            }
            room.room().config.disableNuke = !Boolean.parseBoolean(args[0]);
            broadcastServerInfo(room);
            return false;
        }
    }

    class IncomeCallback implements ChatCommandContextListener {
        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            RoomCommandContext room = roomContext(context);
            if (room == null || room.room().isGaming() || !context.player().isAdmin
                    || args.length < 1) {
                return false;
            }
            float income = Float.parseFloat(args[0]);
            if (income > 100 || income < 0) {
                income = 1;
            }
            room.room().config.income = income;
            broadcastServerInfo(room);
            return false;
        }
    }

    class CreditsCallback implements ChatCommandContextListener {
        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            RoomCommandContext room = roomContext(context);
            if (room == null || room.room().isGaming() || !context.player().isAdmin
                    || args.length < 1) {
                return false;
            }
            room.room().config.credits = Integer.parseInt(args[0]);
            broadcastServerInfo(room);
            return false;
        }
    }

    class SyncCallback implements ChatCommandContextListener {
        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            RoomCommandContext room = roomContext(context);
            if (room != null && room.room().isGaming()) {
                room.room().vote.submitVoting(room.room()::syncGame,
                        "sync", "有玩家发起了同步！输入-y或者-n来投票！", 15);
            }
            return false;
        }
    }

    class AgreeCallback implements ChatCommandContextListener {
        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            RoomCommandContext room = roomContext(context);
            if (room == null) {
                return false;
            }
            if (room.room().vote.disabledVote) {
                context.sendServerMessage("投票已禁用！");
            } else if (room.room().vote.isVoting) {
                context.sendServerMessage(room.room().vote.agree(context.player().playerIndex)
                        ? LangUtil.getString("nostop.vote.submit")
                        : LangUtil.getString("nostop.vote.alreadySubmit"));
            } else {
                context.sendServerMessage(LangUtil.getString("nostop.vote.noCurrentVote"));
            }
            return false;
        }
    }

    class DisagreeCallback implements ChatCommandContextListener {
        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            RoomCommandContext room = roomContext(context);
            if (room == null) {
                return false;
            }
            if (room.room().vote.disabledVote) {
                context.sendServerMessage("投票已禁用！");
            } else if (room.room().vote.isVoting) {
                context.sendServerMessage(room.room().vote.disagree(context.player().playerIndex)
                        ? LangUtil.getString("nostop.vote.submit")
                        : LangUtil.getString("nostop.vote.alreadySubmit"));
            } else {
                context.sendServerMessage(LangUtil.getString("nostop.vote.noCurrentVote"));
            }
            return false;
        }
    }

    class ChksumCallback implements ChatCommandContextListener {
        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            RoomCommandContext room = roomContext(context);
            if (room == null) {
                return false;
            }
            try {
                room.room().broadcast(
                        UniversalPacket.syncCheckSum(room.room().getCurrentStep()));
            } catch (IOException e) {
                LOG.error("Error sending checksum request", e);
            }
            return false;
        }
    }

    class PingCallback implements ChatCommandContextListener {
        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            RoomCommandContext room = roomContext(context);
            if (room == null || args.length < 2) {
                return false;
            }
            try {
                float x = Float.parseFloat(args[0]);
                float y = Float.parseFloat(args[1]);
                room.room().broadcast(UniversalPacket.gamePing(room.room(),
                        context.player().playerIndex, PingType.happy, x, y));
            } catch (IOException e) {
                LOG.error("Error sending ping packet", e);
            }
            return false;
        }
    }

    static class StateCallback implements ChatCommandContextListener {
        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            context.sendServerMessage(buildStateMessage(context.connectionCount(),
                    context.activeThreadCount(), context.threadPoolCount()));
            return false;
        }

        private static String buildStateMessage(int connections, int activeThreads,
                                                int threadPoolCount) {
            StringBuilder build = new StringBuilder();
            build.append("- State - \n");
            build.append("RAM Usage: ").append(Runtime.getRuntime().freeMemory() / 10240)
                    .append("M/").append(Runtime.getRuntime().totalMemory() / 10240)
                    .append("M\n");
            build.append("Connections: ").append(connections);
            build.append("ThreadManager Tasks: ").append(activeThreads).append("/")
                    .append(threadPoolCount);
            return build.toString();
        }
    }

    class PlayerListCallback implements ChatCommandContextListener {
        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            RoomCommandContext room = roomContext(context);
            if (room == null) {
                return false;
            }
            StringBuilder buffer = new StringBuilder("- Players -\n");
            for (ServerRoomConnection connection : room.room().connectionManager.getConnections()) {
                buffer.append(String.format("%s (Team %d) (%d ms)\n",
                        connection.player.name,
                        connection.player.team,
                        System.currentTimeMillis() - connection.pingTime));
            }
            context.sendServerMessage(buffer.toString());
            return false;
        }
    }

    class SurrenderCallback implements ChatCommandContextListener {
        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            RoomCommandContext room = roomContext(context);
            if (room == null || context.player().isSurrounded) {
                return false;
            }
            try {
                room.room().broadcast(UniversalPacket.gameSurrounder(
                        room.room(), context.player().playerIndex));
                room.room().connectionManager.broadcastServerMessage(
                        String.format("Player %s surrounded!", context.player().name));
                context.player().isSurrounded = true;
            } catch (IOException e) {
                LOG.error("Error sending surrender packet", e);
            }
            return false;
        }
    }

    static class AfkCallback implements ChatCommandContextListener {
        @Override
        public boolean onSend(ChatCommandContext context, String[] args) {
            RoomCommandContext room = roomContext(context);
            if (room == null || context.player() == room.room().playerManager.getAdmin()) {
                return false;
            }
            room.room().vote.disabledVote = true;
            room.room().vote.submitVoting(() -> {
                NetworkPlayer formerAdmin = room.room().playerManager.getAdmin();
                if (formerAdmin != null) {
                    formerAdmin.giveAdmin(context.player().playerIndex);
                    formerAdmin.updateServerInfo();
                }
                context.player().updateServerInfo();
            }, "afk", LangUtil.getFormatString("chat.vote.afk", context.player().name), 30);
            return false;
        }
    }

    @Override
    public void onLoad() {
        getLogger().info("CoreCommandPlugin::onLoad()");
        CommandManager manager = Rukkit.getCommandManager();
        register(manager, "help", LangUtil.getString("chat.help"), 1, false,
                new HelpCallback());
        register(manager, "state", LangUtil.getString("chat.state"), 0, false,
                new StateCallback());
        register(manager, "version", LangUtil.getString("chat.version"), 0, false,
                this);
        register(manager, "t", LangUtil.getString("chat.t"), 1, false,
                new TeamChatCallback());
        register(manager, "maps", LangUtil.getString("chat.maps"), 1, false,
                new MapsCallback(0));
        register(manager, "map", LangUtil.getString("chat.map"), 1, true,
                new MapsCallback(1));
        register(manager, "cmaps", LangUtil.getString("chat.cmaps"), 1, false,
                new CustomMapsCallback(0));
        register(manager, "cmap", LangUtil.getString("chat.cmap"), 1, true,
                new CustomMapsCallback(1));
        register(manager, "kick", LangUtil.getString("chat.kick"), 1, true,
                new KickCallback());
        register(manager, "team", LangUtil.getString("chat.team"), 2, true,
                new TeamCallback(0));
        register(manager, "self_team", LangUtil.getString("chat.self_team"), 1, false,
                new TeamCallback(1));
        register(manager, "move", LangUtil.getString("chat.move"), 3, true,
                new MoveCallback(0));
        register(manager, "self_move", LangUtil.getString("chat.self_move"), 2, false,
                new MoveCallback(1));
        register(manager, "qc", LangUtil.getString("chat.qc"), 1, false,
                new QcCallback());
        register(manager, "fog", LangUtil.getString("chat.fog"), 1, true,
                new SetFogCallback());
        register(manager, "nukes", LangUtil.getString("chat.nukes"), 1, true,
                new NukeCallback());
        register(manager, "startingunits", LangUtil.getString("chat.startingunits"), 1, true,
                new StartingUnitCallback());
        register(manager, "income", LangUtil.getString("chat.income"), 1, true,
                new IncomeCallback());
        register(manager, "share", LangUtil.getString("chat.share"), 1, false,
                new ShareCallback());
        register(manager, "credits", LangUtil.getString("chat.credits"), 1, true,
                new CreditsCallback());
        register(manager, "start", LangUtil.getString("chat.start"), 1, true,
                new StartCallback());
        register(manager, "sync", LangUtil.getString("chat.sync"), 0, true,
                new SyncCallback());
        register(manager, "i", LangUtil.getString("chat.i"), 1, false,
                new InfoCallback());
        register(manager, "chksum", LangUtil.getString("chat.chksum"), 0, false,
                new ChksumCallback());
        register(manager, "maping", LangUtil.getString("chat.maping"), 2, false,
                new PingCallback());
        register(manager, "list", LangUtil.getString("chat.list"), 0, false,
                new PlayerListCallback());
        register(manager, "surrender", LangUtil.getString("chat.surrender"), 0, false,
                new SurrenderCallback());
        register(manager, "afk", LangUtil.getString("chat.afk"), 0, false,
                new AfkCallback());
        register(manager, "y", LangUtil.getString("nostop.y"), 0, false,
                new AgreeCallback());
        register(manager, "n", LangUtil.getString("nostop.n"), 0, false,
                new DisagreeCallback());
        getPluginManager().registerEventListener(new CommandEventListener(), this);
    }

    @Override
    public void onEnable() {
        getLogger().info("CoreCommandPlugin::onEnable()");
    }

    @Override
    public void onDisable() {
        // No command-specific resources.
    }

    @Override
    public void onStart() {
        // No command-specific resources.
    }

    @Override
    public void onDone() {
        // No command-specific resources.
    }
}
