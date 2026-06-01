package com.uxplima.uxmlib.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/**
 * Resolution is exercised against MockBukkit's real sound registry, which round-trips known keys to a
 * {@link Sound} and yields {@code null} for unknown keys.
 */
class SoundsTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void resolvesABareKeyAgainstTheMinecraftNamespace() {
        Sound expected = Registry.SOUNDS.get(NamespacedKey.minecraft("block.note_block.pling"));
        assertThat(Sounds.resolve("block.note_block.pling")).contains(expected);
    }

    @Test
    void resolvesAnExplicitlyNamespacedKey() {
        Sound expected = Registry.SOUNDS.get(NamespacedKey.minecraft("block.note_block.pling"));
        assertThat(Sounds.resolve("minecraft:block.note_block.pling")).contains(expected);
    }

    @Test
    void resolveIsCaseInsensitiveForConvenience() {
        Sound expected = Registry.SOUNDS.get(NamespacedKey.minecraft("block.note_block.pling"));
        assertThat(Sounds.resolve("BLOCK.NOTE_BLOCK.PLING")).contains(expected);
    }

    @Test
    void unknownKeyYieldsEmptyRatherThanThrowing() {
        assertThat(Sounds.resolve("block.note_block.does_not_exist")).isEmpty();
        assertThat(Sounds.resolve("minecraft:nope")).isEmpty();
    }

    @Test
    void syntacticallyInvalidKeyYieldsEmptyRatherThanThrowing() {
        assertThat(Sounds.resolve("not a key")).isEmpty();
        assertThat(Sounds.resolve("two:colons:here")).isEmpty();
        assertThat(Sounds.resolve("")).isEmpty();
        assertThat(Sounds.resolve("   ")).isEmpty();
    }
}
