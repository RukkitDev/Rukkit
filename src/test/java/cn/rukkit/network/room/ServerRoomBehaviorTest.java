/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.room;

import cn.rukkit.Rukkit;
import cn.rukkit.config.RoundConfig;
import cn.rukkit.config.RukkitConfig;
import cn.rukkit.game.SaveData;
import cn.rukkit.network.NetworkRoom;
import cn.rukkit.network.command.GameCommand;
import cn.rukkit.service.ThreadManager;
import java.lang.reflect.Field;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerRoomBehaviorTest {
    private Object previousConfig;
    private Object previousRound;
    private Object previousThreadManager;
    private Object previousDefaultSave;
    private ThreadManager testThreadManager;

    @BeforeEach
    void installTestConfiguration() throws ReflectiveOperationException {
        previousConfig = setStatic("config", new RukkitConfig());
        previousRound = setStatic("round", new RoundConfig());
        testThreadManager = new ThreadManager(1);
        previousThreadManager = setStatic("threadManager", testThreadManager);
        SaveData defaultSave = new SaveData();
        defaultSave.arr = new byte[0];
        previousDefaultSave = setStatic("defaultSave", defaultSave);
    }

    @AfterEach
    void restoreConfiguration() throws ReflectiveOperationException {
        testThreadManager.shutdown();
        setStatic("config", previousConfig);
        setStatic("round", previousRound);
        setStatic("threadManager", previousThreadManager);
        setStatic("defaultSave", previousDefaultSave);
    }

    @Test
    void initialStateMatchesLegacyRoom() {
        NetworkRoom legacy = new NetworkRoom(2);
        ServerRoom migrated = new ServerRoom(2);

        assertEquals(legacy.roomId, migrated.roomId);
        assertEquals(legacy.currentStep, migrated.currentStep);
        assertEquals(legacy.stepRate, migrated.stepRate);
        assertEquals(legacy.playerManager.getMaxPlayer(), migrated.playerManager.getMaxPlayer());
        assertEquals(legacy.connectionManager.size(), migrated.connectionManager.size());
        assertEquals(legacy.toString(), migrated.toString());
        assertFalse(legacy.isPaused());
        assertFalse(migrated.isPaused());
        assertFalse(legacy.isGaming());
        assertFalse(migrated.isGaming());
    }

    @Test
    void pauseAndRunningStateMatchLegacyRoom() {
        NetworkRoom legacy = new NetworkRoom(2);
        ServerRoom migrated = new ServerRoom(2);

        legacy.setPaused(true);
        migrated.setPaused(true);
        assertTrue(legacy.isPaused());
        assertTrue(migrated.isPaused());

        legacy.currentStep = 10;
        migrated.currentStep = 10;
        assertEquals(legacy.isGaming(), migrated.isGaming());
        assertTrue(migrated.isGaming());

        legacy.setPaused(false);
        migrated.setPaused(false);
        assertEquals(legacy.isPaused(), migrated.isPaused());
    }

    @Test
    void commandQueueModeMatchesLegacyRoom() throws ReflectiveOperationException {
        RukkitConfig config = (RukkitConfig) getStatic("config");
        config.useCommandQuere = true;
        NetworkRoom legacy = new NetworkRoom(2);
        ServerRoom migrated = new ServerRoom(2);
        GameCommand command = new GameCommand();
        command.arr = new byte[] {1, 2, 3};

        legacy.addCommand(command);
        migrated.addCommand(command);

        assertEquals(queueSize(NetworkRoom.class, legacy), queueSize(ServerRoom.class, migrated));
        assertEquals(1, queueSize(ServerRoom.class, migrated));
    }

    @Test
    void pausedRoomDoesNotAcceptNewCommands() throws ReflectiveOperationException {
        RukkitConfig config = (RukkitConfig) getStatic("config");
        config.useCommandQuere = true;
        ServerRoom room = new ServerRoom(2);
        room.setPaused(true);

        GameCommand command = new GameCommand();
        command.arr = new byte[] {4, 5, 6};
        room.addCommand(command);

        assertEquals(0, queueSize(ServerRoom.class, room));
        room.stopGame();
    }

    @Test
    void stopGameClearsPendingCommands() throws ReflectiveOperationException {
        RukkitConfig config = (RukkitConfig) getStatic("config");
        config.useCommandQuere = true;
        ServerRoom room = new ServerRoom(2);
        GameCommand command = new GameCommand();
        command.arr = new byte[] {7, 8, 9};
        room.addCommand(command);

        assertEquals(1, queueSize(ServerRoom.class, room));
        room.stopGame(true);

        assertEquals(0, queueSize(ServerRoom.class, room));
        assertTrue(room.isPaused());
    }

    @Test
    void syncFlushesPendingCommandsBeforeRequestingSave() throws Exception {
        RukkitConfig config = (RukkitConfig) getStatic("config");
        config.useCommandQuere = true;
        ServerRoom room = new ServerRoom(2);
        GameCommand command = new GameCommand();
        command.arr = new byte[] {10, 11, 12};
        room.addCommand(command);

        room.syncGame();

        assertEquals(0, queueSize(ServerRoom.class, room));
        assertTrue(room.isPaused());
        room.stopGame();
    }

    @Test
    void syncWaitDoesNotOccupyTheOnlySharedWorkerThread() throws Exception {
        ServerRoom room = new ServerRoom(2);
        room.syncGame();

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
        while (testThreadManager.getActiveThreadCount() == 0
                && System.nanoTime() < deadline) {
            Thread.yield();
        }

        Future<?> marker = testThreadManager.submit(() -> { });
        marker.get(1, TimeUnit.SECONDS);

        assertTrue(room.isPaused());
        room.stopGame();
    }

    @Test
    void checksumWaitDoesNotOccupyTheOnlySharedWorkerThread() throws Exception {
        ServerRoom room = new ServerRoom(2);
        room.doChecksum();

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
        while (testThreadManager.getActiveThreadCount() == 0
                && System.nanoTime() < deadline) {
            Thread.yield();
        }

        Future<?> marker = testThreadManager.submit(() -> { });
        marker.get(1, TimeUnit.SECONDS);

        room.stopGame();
    }

    private static int queueSize(Class<?> roomType, Object room) throws ReflectiveOperationException {
        Field queue = roomType.getDeclaredField("commandQuere");
        queue.setAccessible(true);
        return ((RoomCommandQueue) queue.get(room)).size();
    }

    private static Object getStatic(String name) throws ReflectiveOperationException {
        Field field = Rukkit.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    private static Object setStatic(String name, Object value) throws ReflectiveOperationException {
        Field field = Rukkit.class.getDeclaredField(name);
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, value);
        return previous;
    }
}
