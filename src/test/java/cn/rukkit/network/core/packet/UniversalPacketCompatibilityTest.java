/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 *
 * This project uses GNU Affero General Public License v3.0.You can find this license in the following link.
 * 本项目使用 GNU Affero General Public License v3.0 许可证，你可以在下方链接查看:
 *
 * https://github.com/RukkitDev/Rukkit/blob/master/LICENSE
 */

package cn.rukkit.network.core.packet;

import cn.rukkit.Rukkit;
import cn.rukkit.config.RoundConfig;
import cn.rukkit.config.RukkitConfig;
import cn.rukkit.game.PingType;
import cn.rukkit.network.command.GameCommand;
import cn.rukkit.network.io.GameInputStream;
import java.lang.reflect.Field;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Locks down the wire representation while packet builders move packages.
 */
class UniversalPacketCompatibilityTest {
    private Object previousConfig;

    @BeforeEach
    void installTestConfiguration() throws ReflectiveOperationException {
        Field field = Rukkit.class.getDeclaredField("config");
        field.setAccessible(true);
        previousConfig = field.get(null);
        RukkitConfig config = new RukkitConfig();
        config.UUID = "test-server-uuid";
        field.set(null, config);
    }

    @AfterEach
    void restoreConfiguration() throws ReflectiveOperationException {
        Field field = Rukkit.class.getDeclaredField("config");
        field.setAccessible(true);
        field.set(null, previousConfig);
    }

    @FunctionalInterface
    private interface PacketFactory<T> {
        T create() throws IOException;
    }

    @Test
    void chatMatchesLegacyPacket() throws IOException {
        assertEquivalent(
                () -> cn.rukkit.network.packet.Packet.chat("server", "hello", -1),
                () -> UniversalPacket.chat("server", "hello", -1));
    }

    @Test
    void gameCommandMatchesLegacyPacket() throws IOException {
        GameCommand command = new GameCommand();
        command.arr = new byte[] {1, 0, 0, 0, 2, 3, 5, 8};

        assertEquivalent(
                () -> cn.rukkit.network.packet.Packet.gameCommand(42, command),
                () -> UniversalPacket.gameCommand(42, command));
    }

    @Test
    void gameCommandPacketSupportsMultipleOriginalStyleBlocks() throws IOException {
        GameCommand first = new GameCommand();
        first.arr = new byte[] {1, 2, 3};
        GameCommand second = new GameCommand();
        second.arr = new byte[] {5, 8, 13};
        List<GameCommand> commands = List.of(first, second);

        assertEquivalent(
                () -> cn.rukkit.network.packet.Packet.gameCommands(42, commands),
                () -> UniversalPacket.gameCommands(42, commands));

        GameInputStream input = new GameInputStream(
                UniversalPacket.gameCommands(42, commands));
        assertEquals(42, input.readInt());
        assertEquals(2, input.readInt());
        assertArrayEquals(first.arr, input.getBlockRaw("c"));
        assertArrayEquals(second.arr, input.getBlockRaw("c"));
    }

    @Test
    void simplePacketsMatchLegacyPacket() throws IOException {
        assertEquivalent(
                () -> cn.rukkit.network.packet.Packet.preRegister(),
                () -> UniversalPacket.preRegister());
        assertEquivalent(
                () -> cn.rukkit.network.packet.Packet.emptyCommand(42),
                () -> UniversalPacket.emptyCommand(42));
        assertEquivalent(
                () -> cn.rukkit.network.packet.Packet.sandSave(),
                () -> UniversalPacket.sandSave());
        assertEquivalent(
                () -> cn.rukkit.network.packet.Packet.kick("unsupported"),
                () -> UniversalPacket.kick("unsupported"));
        assertEquivalent(
                () -> cn.rukkit.network.packet.Packet.packetQuestion(7, "continue?"),
                () -> UniversalPacket.packetQuestion(7, "continue?"));
        assertEquivalent(
                () -> cn.rukkit.network.packet.Packet.packetReturnToBattleroom(),
                () -> UniversalPacket.packetReturnToBattleroom());
    }

    @Test
    void startPacketUsesTheProvidedRoomConfiguration() throws IOException {
        RoundConfig roomConfig = new RoundConfig();
        roomConfig.mapType = 0;
        roomConfig.mapName = "room-specific-map";

        GameInputStream input = new GameInputStream(UniversalPacket.startGame(roomConfig));
        assertEquals(0, input.readByte());
        assertEquals(0, input.readInt());
        assertEquals("maps/skirmish/room-specific-map.tmx", input.readString());
    }

    @Test
    void saveAndChecksumPacketsMatchLegacyPacket() throws IOException {
        byte[] save = {0, 1, 2, 3, 8, 13, 21};

        assertEquivalent(
                () -> cn.rukkit.network.packet.Packet.sendSave(123, save, false),
                () -> UniversalPacket.sendSave(123, save, false));
        assertEquivalent(
                () -> cn.rukkit.network.packet.Packet.sendSave(123, save, true),
                () -> UniversalPacket.sendSave(123, save, true));
        assertEquivalent(
                () -> cn.rukkit.network.packet.Packet.syncCheckSum(123),
                () -> UniversalPacket.syncCheckSum(123));
    }

    @Test
    void savePacketContainsTheGameSaveBlock() throws IOException {
        byte[] save = {0, 1, 2, 3, 8, 13, 21};
        GameInputStream input = new GameInputStream(UniversalPacket.sendSave(123, save, false));

        input.readByte();
        input.readInt();
        input.readInt();
        input.readFloat();
        input.readFloat();
        assertFalse(input.readBoolean());
        assertFalse(input.readBoolean());

        assertArrayEquals(save, input.getBlockRaw("gameSave"));
    }

    @Test
    void gameActionPacketsMatchLegacyPacket() throws IOException {
        assertEquivalent(
                () -> cn.rukkit.network.packet.Packet.gamePing(123, 2, PingType.attack, 12.5f, -4.25f),
                () -> UniversalPacket.gamePing(123, 2, PingType.attack, 12.5f, -4.25f));
        assertEquivalent(
                () -> cn.rukkit.network.packet.Packet.gameSummon(123, "custom-unit", 12.5f, -4.25f, 2),
                () -> UniversalPacket.gameSummon(123, "custom-unit", 12.5f, -4.25f, 2));
        assertEquivalent(
                () -> cn.rukkit.network.packet.Packet.gameSurrounder(123, 2),
                () -> UniversalPacket.gameSurrounder(123, 2));
    }

    private static void assertEquivalent(PacketFactory<cn.rukkit.network.packet.Packet> legacyFactory,
                                          PacketFactory<Packet> migratedFactory) throws IOException {
        cn.rukkit.network.packet.Packet legacy = legacyFactory.create();
        Packet migrated = migratedFactory.create();

        assertEquals(legacy.type, migrated.type);
        assertArrayEquals(legacy.bytes, migrated.bytes,
                () -> "packet payload differs: legacy=" + Arrays.toString(legacy.bytes)
                        + ", migrated=" + Arrays.toString(migrated.bytes));
    }
}
