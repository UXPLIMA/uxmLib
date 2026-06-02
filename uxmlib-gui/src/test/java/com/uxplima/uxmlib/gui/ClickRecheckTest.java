package com.uxplima.uxmlib.gui;

import static org.assertj.core.api.Assertions.assertThat;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/**
 * Item 39: the anti-desync re-check. Before a click action runs, the slot must still hold the icon the
 * click targeted; if a dynamic/animated item changed between render and click, the action is skipped. The
 * predicate is pure; MockBukkit is here only so the {@link ItemStack} fixtures can be built.
 */
class ClickRecheckTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void matchesWhenTheSlotStillHoldsTheClickedIcon() {
        ItemStack clicked = new ItemStack(Material.DIAMOND_SWORD);
        ItemStack current = new ItemStack(Material.DIAMOND_SWORD);

        assertThat(ClickRecheck.stillMatches(clicked, current)).isTrue();
    }

    @Test
    void rejectsWhenTheIconChangedSinceRender() {
        ItemStack clicked = new ItemStack(Material.DIAMOND_SWORD);
        ItemStack current = new ItemStack(Material.GOLDEN_SWORD); // an animated item advanced a frame

        assertThat(ClickRecheck.stillMatches(clicked, current)).isFalse();
    }

    @Test
    void rejectsWhenTheSlotChangedAmount() {
        ItemStack clicked = new ItemStack(Material.ARROW, 1);
        ItemStack current = new ItemStack(Material.ARROW, 16);

        assertThat(ClickRecheck.stillMatches(clicked, current)).isFalse();
    }

    @Test
    void treatsAnEmptySlotAsAir() {
        ItemStack clicked = new ItemStack(Material.AIR);

        // A null live slot means "empty", which equals an AIR snapshot.
        assertThat(ClickRecheck.stillMatches(clicked, null)).isTrue();
    }

    @Test
    void rejectsWhenTheSlotEmptiedAfterRender() {
        ItemStack clicked = new ItemStack(Material.EMERALD);

        assertThat(ClickRecheck.stillMatches(clicked, null)).isFalse();
    }
}
