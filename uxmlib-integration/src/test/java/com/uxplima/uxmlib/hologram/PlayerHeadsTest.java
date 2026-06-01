package com.uxplima.uxmlib.hologram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/** Verifies player-head item construction (the head item, not live spawning). */
class PlayerHeadsTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void buildsAPlayerHeadForAUuid() {
        ItemStack head = PlayerHeads.ofUuid(new java.util.UUID(0, 42));
        assertThat(head.getType()).isEqualTo(Material.PLAYER_HEAD);
        assertThat(head.getItemMeta()).isInstanceOf(SkullMeta.class);
    }

    @Test
    void buildsAPlayerHeadFromATexture() {
        ItemStack head = PlayerHeads.fromTexture("eyJ0ZXh0dXJlcyI6e319"); // a base64 blob
        assertThat(head.getType()).isEqualTo(Material.PLAYER_HEAD);
    }

    @Test
    void rejectsABlankTexture() {
        assertThatThrownBy(() -> PlayerHeads.fromTexture("  ")).isInstanceOf(IllegalArgumentException.class);
    }
}
