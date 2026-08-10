/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 */

package cn.rukkit.network.core.handler;

import cn.rukkit.Rukkit;
import cn.rukkit.config.RoundConfig;
import cn.rukkit.config.RukkitConfig;
import cn.rukkit.event.EventHandler;
import cn.rukkit.event.EventListener;
import cn.rukkit.event.EventListenerContainer;
import cn.rukkit.event.server.ServerQuestionRespondEvent;
import cn.rukkit.game.NetworkPlayer;
import cn.rukkit.network.ConnectionState;
import cn.rukkit.network.core.packet.Packet;
import cn.rukkit.network.core.packet.PacketType;
import cn.rukkit.network.io.GameInputStream;
import cn.rukkit.network.io.GameOutputStream;
import cn.rukkit.network.room.ServerGlobalConnectionManager;
import cn.rukkit.network.room.ServerRoom;
import cn.rukkit.network.room.ServerRoomConnection;
import cn.rukkit.network.room.ServerRoomManager;
import cn.rukkit.service.ThreadManager;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerLowRiskHandlerBehaviorTest {
    private final List<EmbeddedChannel> channels = new ArrayList<>();
    private final List<ServerConnectionHandler> handlers = new ArrayList<>();
    private Object previousConfig;
    private Object previousRound;
    private Object previousThreadManager;
    private ThreadManager testThreadManager;
    private ServerRoomManager roomManager;
    private ServerGlobalConnectionManager globalConnectionManager;

    @BeforeEach
    void installTestRuntime() throws ReflectiveOperationException {
        RukkitConfig config = new RukkitConfig();
        config.maxRoom = 1;
        config.maxPlayer = 2;
        config.isDebug = false;
        previousConfig = setStatic("config", config);
        previousRound = setStatic("round", new RoundConfig());
        testThreadManager = new ThreadManager(2);
        previousThreadManager = setStatic("threadManager", testThreadManager);

        roomManager = new ServerRoomManager(Rukkit.getRoundConfig(), 1);
        globalConnectionManager = new ServerGlobalConnectionManager(roomManager);
        ServerQuestionRespondEvent.getListenerList().clear();
    }

    @AfterEach
    void restoreTestRuntime() throws ReflectiveOperationException {
        ServerQuestionRespondEvent.getListenerList().clear();
        for (ServerConnectionHandler handler : handlers) {
            handler.stopTimeout();
        }
        for (EmbeddedChannel channel : channels) {
            channel.finishAndReleaseAll();
        }
        testThreadManager.shutdown();

        setStatic("config", previousConfig);
        setStatic("round", previousRound);
        setStatic("threadManager", previousThreadManager);
    }

    @Test
    void disconnectUsesClientReasonAndCleansUpCoreRegistries() throws Exception {
        ConnectionFixture fixture = newConnection(ConnectionState.IN_ROOM);

        fixture.channel.writeInbound(stringPacket(PacketType.DISCONNECT, "client left"));

        assertEquals(ConnectionState.DISCONNECTED, fixture.handler.getState());
        assertEquals("client left", fixture.handler.getDisconnectReason());
        assertFalse(fixture.channel.isOpen());
        assertEquals(0, globalConnectionManager.size());
        assertEquals(0, fixture.room.connectionManager.size());
    }

    @Test
    void questionResponsePublishesTheMasterEventPayload() throws Exception {
        QuestionProbe probe = new QuestionProbe();
        Method method = QuestionProbe.class.getDeclaredMethod(
                "onQuestion", ServerQuestionRespondEvent.class);
        ServerQuestionRespondEvent.getListenerList().registerListener(
                new EventListenerContainer(null, method, probe));
        ConnectionFixture fixture = newConnection(ConnectionState.IN_ROOM);

        fixture.channel.writeInbound(questionResponsePacket(42, "yes"));

        assertSame(fixture.connection.player, probe.player);
        assertEquals(42, probe.questionId);
        assertEquals("yes", probe.response);
    }

    @Test
    void randyBroadcastMatchesMasterMessage() throws Exception {
        ConnectionFixture fixture = newConnection(ConnectionState.IN_GAME);

        fixture.channel.writeInbound(new Packet(PacketType.READY, new byte[0]));

        Packet message = fixture.channel.readOutbound();
        assertNotNull(message);
        assertEquals(PacketType.SEND_CHAT, message.type);
        assertEquals("Player 'Alice' is randy.", new GameInputStream(message).readString());
    }

    @Test
    void lowRiskHandlersKeepMasterStateRestrictions() {
        assertEquals(List.of(ConnectionState.PRE_REGISTERED,
                        ConnectionState.IN_ROOM,
                        ConnectionState.IN_GAME),
                new ServerDisconnectHandler().getAllowedStates());
        assertEquals(List.of(ConnectionState.IN_ROOM, ConnectionState.IN_GAME),
                new ServerQuestionResponseHandler().getAllowedStates());
        assertEquals(List.of(ConnectionState.IN_ROOM, ConnectionState.IN_GAME),
                new ServerRandyHandler().getAllowedStates());
    }

    private ConnectionFixture newConnection(ConnectionState state) {
        ServerPacketHandlerManager handlerManager = new ServerPacketHandlerManager();
        handlerManager.registerInternalHandler(roomManager, globalConnectionManager);
        ServerConnectionHandler handler = new ServerConnectionHandler(
                handlerManager, globalConnectionManager::discard);
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        handlers.add(handler);
        channels.add(channel);

        ServerRoom room = roomManager.getRoom(0);
        ServerRoomConnection connection = new ServerRoomConnection(handler, room);
        connection.player = new NetworkPlayer(connection);
        connection.player.name = "Alice";
        connection.player.uuid = "low-risk-handler-test";
        room.connectionManager.add(connection);
        globalConnectionManager.add(connection);
        handler.setConn(connection);
        handler.setState(state);
        return new ConnectionFixture(handler, channel, room, connection);
    }

    private static Packet stringPacket(int type, String value) throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeString(value);
        return output.createPacket(type);
    }

    private static Packet questionResponsePacket(int questionId, String response)
            throws IOException {
        GameOutputStream output = new GameOutputStream();
        output.writeByte(0);
        output.writeInt(questionId);
        output.writeString(response);
        return output.createPacket(PacketType.QUESTION_RESPONSE);
    }

    private static Object setStatic(String name, Object value)
            throws ReflectiveOperationException {
        Field field = Rukkit.class.getDeclaredField(name);
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, value);
        return previous;
    }

    private record ConnectionFixture(ServerConnectionHandler handler,
                                     EmbeddedChannel channel,
                                     ServerRoom room,
                                     ServerRoomConnection connection) {
    }

    private static final class QuestionProbe implements EventListener {
        private NetworkPlayer player;
        private int questionId;
        private String response;

        @EventHandler
        public void onQuestion(ServerQuestionRespondEvent event) {
            player = event.getPlayer();
            questionId = event.getQid();
            response = event.getRespondMessage();
        }
    }
}
