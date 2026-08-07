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
import cn.rukkit.network.NetworkRoom;
import cn.rukkit.network.command.GameCommand;
import java.lang.reflect.Field;
import java.util.LinkedList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerRoomBehaviorTest {
    private Object previousConfig;
    private Object previousRound;

    @BeforeEach
    void installTestConfiguration() throws ReflectiveOperationException {
        previousConfig = setStatic("config", new RukkitConfig());
        previousRound = setStatic("round", new RoundConfig());
    }

    @AfterEach
    void restoreConfiguration() throws ReflectiveOperationException {
        setStatic("config", previousConfig);
        setStatic("round", previousRound);
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

    private static int queueSize(Class<?> roomType, Object room) throws ReflectiveOperationException {
        Field queue = roomType.getDeclaredField("commandQuere");
        queue.setAccessible(true);
        return ((LinkedList<?>) queue.get(room)).size();
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
