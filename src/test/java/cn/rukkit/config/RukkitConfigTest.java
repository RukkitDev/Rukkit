/*
 * Copyright 2020-2022 RukkitDev Team and contributors.
 */

package cn.rukkit.config;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RukkitConfigTest {
    @Test
    void defaultsToLegacyNetworkRuntime() {
        RukkitConfig config = new RukkitConfig();

        assertEquals("legacy", config.getNetworkMode());
        assertFalse(config.isCoreNetworkEnabled());
    }

    @Test
    void readsCoreNetworkModeFromNestedYamlConfig() {
        RukkitConfig config = new Yaml().loadAs(
                "network:\n  mode: CORE\n", RukkitConfig.class);

        assertEquals("core", config.getNetworkMode());
        assertTrue(config.isCoreNetworkEnabled());
    }

    @Test
    void unknownNetworkModeFallsBackToLegacy() {
        RukkitConfig config = new RukkitConfig();
        config.network.mode = "unsupported";

        assertEquals("legacy", config.getNetworkMode());
        assertFalse(config.isCoreNetworkEnabled());
    }
}
