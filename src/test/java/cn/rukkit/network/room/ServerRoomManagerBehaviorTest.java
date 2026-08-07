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
import cn.rukkit.game.NetworkPlayer;
import java.lang.reflect.Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerRoomManagerBehaviorTest {
    private Object previousConfig;
    private Object previousRound;

    @BeforeEach
    void installTestConfiguration() throws ReflectiveOperationException {
        RukkitConfig config = new RukkitConfig();
        config.maxRoom = 3;
        config.maxPlayer = 2;
        previousConfig = setStatic("config", config);
        previousRound = setStatic("round", new RoundConfig());
    }

    @AfterEach
    void restoreConfiguration() throws ReflectiveOperationException {
        setStatic("config", previousConfig);
        setStatic("round", previousRound);
    }

    @Test
    void createsAndExposesMasterCompatibleRoomList() {
        ServerRoomManager manager = new ServerRoomManager(Rukkit.getRoundConfig(), 3);

        assertEquals(3, manager.roomList.size());
        assertSame(manager.roomList.get(0), manager.getDefaultRoom());
        assertEquals(0, manager.getRoom(0).roomId);
        assertEquals(1, manager.getRoom(1).roomId);
        assertEquals(2, manager.getRoom(2).roomId);
    }

    @Test
    void selectsFirstNonGamingRoomWithCapacity() {
        ServerRoomManager manager = new ServerRoomManager(Rukkit.getRoundConfig(), 3);
        ServerRoom first = manager.getRoom(0);
        first.playerManager.add(activePlayer());
        first.playerManager.add(activePlayer());

        ServerRoom available = manager.getAvailableRoom();

        assertNotNull(available);
        assertEquals(1, available.roomId);
    }

    @Test
    void resetRebuildsRoomsWithoutConcurrentModification() {
        ServerRoomManager manager = new ServerRoomManager(Rukkit.getRoundConfig(), 3);
        ServerRoom oldRoom = manager.getRoom(0);

        manager.resetAllRooms();

        assertEquals(3, manager.roomList.size());
        assertTrue(oldRoom != manager.getRoom(0));
        assertEquals(0, manager.getRoom(0).roomId);
    }

    private static NetworkPlayer activePlayer() {
        NetworkPlayer player = new NetworkPlayer();
        player.isEmpty = false;
        return player;
    }

    private static Object setStatic(String name, Object value) throws ReflectiveOperationException {
        Field field = Rukkit.class.getDeclaredField(name);
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, value);
        return previous;
    }
}
