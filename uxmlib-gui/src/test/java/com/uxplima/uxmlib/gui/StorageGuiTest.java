package com.uxplima.uxmlib.gui;

import static org.assertj.core.api.Assertions.assertThat;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/** Covers the StorageGui: it allows take/place by default and keeps its contents readable. */
class StorageGuiTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void allowsTakeAndPlaceByDefault() {
        StorageGui gui = Guis.storage().rows(3).build();
        assertThat(gui.allows(InteractionModifier.ITEM_TAKE)).isTrue();
        assertThat(gui.allows(InteractionModifier.ITEM_PLACE)).isTrue();
        assertThat(gui.allows(InteractionModifier.ITEM_SWAP)).isTrue();
        assertThat(gui.allows(InteractionModifier.ITEM_DROP)).isFalse();
    }

    @Test
    void seedsAndReadsBackContents() {
        StorageGui gui = Guis.storage().rows(1).build();
        ItemStack[] seed = new ItemStack[9];
        seed[2] = new ItemStack(Material.DIAMOND, 4);
        gui.setContents(seed);

        ItemStack[] read = gui.contents();
        assertThat(read[2]).isNotNull();
        assertThat(read[2].getType()).isEqualTo(Material.DIAMOND);
        assertThat(read[2].getAmount()).isEqualTo(4);
        assertThat(read[0]).isNull();
    }

    @Test
    void contentsReflectDirectInventoryEdits() {
        StorageGui gui = Guis.storage().rows(1).build();
        // Simulate a player dropping an item into slot 5.
        gui.getInventory().setItem(5, new ItemStack(Material.EMERALD, 2));

        ItemStack[] read = gui.contents();
        assertThat(read[5]).isNotNull();
        assertThat(read[5].getType()).isEqualTo(Material.EMERALD);
    }

    @Test
    void addItemStoresWhatFitsAndReturnsNoLeftover() {
        StorageGui gui = Guis.storage().rows(1).build();

        var leftover = gui.addItem(new ItemStack(Material.DIAMOND, 4));

        assertThat(leftover).isEmpty();
        assertThat(gui.getInventory().contains(Material.DIAMOND, 4)).isTrue();
    }

    @Test
    void addItemReturnsTheOverflowThatDidNotFit() {
        // A single empty slot fits one full stack; the rest must come back as leftover, not be dropped.
        StorageGui gui = Guis.storage().rows(1).build();
        ItemStack[] seed = new ItemStack[9];
        for (int slot = 1; slot < 9; slot++) {
            seed[slot] = new ItemStack(Material.STONE, 1); // every slot but 0 is occupied by another type
        }
        gui.setContents(seed);

        var leftover = gui.addItem(new ItemStack(Material.DIAMOND, 128));

        assertThat(leftover).hasSize(1);
        ItemStack remainder = java.util.Objects.requireNonNull(leftover.get(0));
        assertThat(remainder.getType()).isEqualTo(Material.DIAMOND);
        assertThat(remainder.getAmount()).isEqualTo(128 - Material.DIAMOND.getMaxStackSize());
    }

    @Test
    @SuppressWarnings("NullAway")
    void addItemRejectsANullEntry() {
        StorageGui gui = Guis.storage().rows(1).build();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> gui.addItem((ItemStack) null))
                .isInstanceOf(NullPointerException.class);
    }
}
