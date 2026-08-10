/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find the license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network;

import cn.rukkit.Rukkit;
import cn.rukkit.config.RoundConfig;
import cn.rukkit.config.RukkitConfig;
import cn.rukkit.game.NetworkPlayer;
import cn.rukkit.game.SaveData;
import cn.rukkit.game.mod.ModManager;
import cn.rukkit.network.core.handler.ServerConnectionHandler;
import cn.rukkit.network.core.handler.ServerPacketHandlerManager;
import cn.rukkit.network.room.ServerRoom;
import cn.rukkit.network.room.ServerRoomConnection;
import cn.rukkit.service.ThreadManager;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkTickBehaviorTest {
    private Object previousConfig;
    private Object previousRound;
    private Object previousThreadManager;
    private Object previousDefaultSave;
    private Object previousModManager;
    private ThreadManager testThreadManager;
    private final List<EmbeddedChannel> channels = new ArrayList<>();

    @BeforeEach
    void installTestConfiguration() throws ReflectiveOperationException {
        RukkitConfig config = new RukkitConfig();
        config.singlePlayerMode = true;
        previousConfig = setStatic("config", config);
        previousRound = setStatic("round", new RoundConfig());
        previousModManager = setStatic("modManager", new ModManager());
        testThreadManager = new ThreadManager(2);
        previousThreadManager = setStatic("threadManager", testThreadManager);
        SaveData defaultSave = new SaveData();
        defaultSave.arr = new byte[0];
        previousDefaultSave = setStatic("defaultSave", defaultSave);
    }

    @AfterEach
    void restoreConfiguration() throws ReflectiveOperationException {
        for (EmbeddedChannel channel : channels) {
            channel.finishAndReleaseAll();
        }
        testThreadManager.shutdown();
        setStatic("config", previousConfig);
        setStatic("round", previousRound);
        setStatic("modManager", previousModManager);
        setStatic("threadManager", previousThreadManager);
        setStatic("defaultSave", previousDefaultSave);
    }

    @Test
    void usesTheOriginalTenFrameNetworkWindow() {
        assertEquals(10, NetworkTick.FRAMES_PER_WINDOW);
        assertEquals(TimeUnit.SECONDS.toNanos(1) / 6,
                NetworkTick.WINDOW_PERIOD_NANOS);
        assertEquals(167, NetworkTick.WINDOW_PERIOD_MILLIS);
    }

    @Test
    void bothRoomImplementationsAdvanceTenFramesPerRunningWindow() {
        LegacyFixture legacy = newLegacyFixture();
        MigratedFixture migrated = newMigratedFixture();

        legacy.room.new GameTask().run();
        migrated.room.new GameTask().run();

        assertEquals(NetworkTick.FRAMES_PER_WINDOW, legacy.room.getCurrentStep());
        assertEquals(NetworkTick.FRAMES_PER_WINDOW, migrated.room.getCurrentStep());
    }

    @Test
    void pausedRoomDoesNotAdvanceAndResumesAtTheSameWindowSize() {
        LegacyFixture legacy = newLegacyFixture();
        MigratedFixture migrated = newMigratedFixture();
        legacy.room.setPaused(true);
        migrated.room.setPaused(true);

        legacy.room.new GameTask().run();
        migrated.room.new GameTask().run();

        assertEquals(0, legacy.room.getCurrentStep());
        assertEquals(0, migrated.room.getCurrentStep());

        legacy.room.setPaused(false);
        migrated.room.setPaused(false);
        legacy.room.new GameTask().run();
        migrated.room.new GameTask().run();

        assertEquals(NetworkTick.FRAMES_PER_WINDOW, legacy.room.getCurrentStep());
        assertEquals(NetworkTick.FRAMES_PER_WINDOW, migrated.room.getCurrentStep());
    }

    @Test
    void noStopTaskUsesTheSameWindowSize() {
        LegacyFixture legacy = newLegacyFixture();
        MigratedFixture migrated = newMigratedFixture();

        legacy.room.new NonStopGameTask().run();
        migrated.room.new NonStopGameTask().run();

        assertEquals(NetworkTick.FRAMES_PER_WINDOW, legacy.room.getCurrentStep());
        assertEquals(NetworkTick.FRAMES_PER_WINDOW, migrated.room.getCurrentStep());
    }

    @Test
    void stoppingEitherRoomCancelsFutureTickAdvancement() throws Exception {
        LegacyFixture legacy = newLegacyFixture();
        MigratedFixture migrated = newMigratedFixture();

        legacy.room.startGame();
        migrated.room.startGame();
        awaitAtLeast(legacy.room::getCurrentStep, NetworkTick.FRAMES_PER_WINDOW);
        awaitAtLeast(migrated.room::getCurrentStep, NetworkTick.FRAMES_PER_WINDOW);

        legacy.room.stopGame();
        migrated.room.stopGame();
        assertEquals(0, legacy.room.getCurrentStep());
        assertEquals(0, migrated.room.getCurrentStep());

        Thread.sleep(NetworkTick.WINDOW_PERIOD_MILLIS * 2L);
        assertEquals(0, legacy.room.getCurrentStep());
        assertEquals(0, migrated.room.getCurrentStep());
    }

    @Test
    void startingTheSameRoomTwiceDoesNotRegisterASecondTicker()
            throws Exception {
        LegacyFixture legacy = newLegacyFixture();
        MigratedFixture migrated = newMigratedFixture();

        legacy.room.startGame();
        migrated.room.startGame();
        Object legacyTask = taskFuture(NetworkRoom.class, legacy.room);
        Object migratedTask = taskFuture(ServerRoom.class, migrated.room);

        legacy.room.startGame();
        migrated.room.startGame();

        assertSame(legacyTask, taskFuture(NetworkRoom.class, legacy.room));
        assertSame(migratedTask, taskFuture(ServerRoom.class, migrated.room));

        legacy.room.stopGame();
        migrated.room.stopGame();
    }

    private LegacyFixture newLegacyFixture() {
        NetworkRoom room = new NetworkRoom(1);
        ConnectionHandler handler = new ConnectionHandler();
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channels.add(channel);
        RoomConnection connection = new RoomConnection(handler, room);
        connection.player = new NetworkPlayer(connection);
        room.connectionManager.add(connection);
        return new LegacyFixture(room, channel);
    }

    private MigratedFixture newMigratedFixture() {
        ServerRoom room = new ServerRoom(1);
        ServerConnectionHandler handler = new ServerConnectionHandler(
                new ServerPacketHandlerManager());
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channels.add(channel);
        ServerRoomConnection connection = new ServerRoomConnection(handler, room);
        connection.player = new NetworkPlayer(connection);
        room.connectionManager.add(connection);
        return new MigratedFixture(room, channel);
    }

    private static void awaitAtLeast(IntSupplier currentStep, int expected)
            throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (currentStep.getAsInt() < expected && System.nanoTime() < deadline) {
            Thread.sleep(5);
        }
        assertTrue(currentStep.getAsInt() >= expected,
                "room did not emit its first network tick in time");
    }

    private static Object setStatic(String name, Object value)
            throws ReflectiveOperationException {
        Field field = Rukkit.class.getDeclaredField(name);
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, value);
        return previous;
    }

    private static Object taskFuture(Class<?> roomType, Object room)
            throws ReflectiveOperationException {
        Field field = roomType.getDeclaredField("gameTaskFuture");
        field.setAccessible(true);
        return field.get(room);
    }

    private record LegacyFixture(NetworkRoom room, EmbeddedChannel channel) {
    }

    private record MigratedFixture(ServerRoom room, EmbeddedChannel channel) {
    }
}
