package com.uxplima.uxmlib.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

/**
 * Verifies the Bukkit-type config codecs round-trip through a real HOCON file. MockBukkit is started so
 * {@link Material}/{@link NamespacedKey}/{@link Color} resolve against a registry.
 */
class ConfigCodecsTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @ConfigSerializable
    record Settings(Material icon, NamespacedKey key, Color colour) {}

    @Test
    void mapsBukkitScalarTypesFromConfig(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.conf");
        Files.writeString(file, "settings { icon = DIAMOND_SWORD, key = \"minecraft:stone\", colour = \"#FF8800\" }\n");

        HoconConfig config = HoconConfig.load(file, ConfigCodecs.bukkit());
        Settings fallback = new Settings(Material.STONE, NamespacedKey.minecraft("air"), Color.WHITE);
        Settings settings = config.getNode("settings", Settings.class, fallback);

        assertThat(settings.icon()).isEqualTo(Material.DIAMOND_SWORD);
        assertThat(settings.key()).isEqualTo(NamespacedKey.minecraft("stone"));
        assertThat(settings.colour()).isEqualTo(Color.fromRGB(0xFF8800));
    }
}
