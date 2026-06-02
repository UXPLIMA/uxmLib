package com.uxplima.uxmlib.gui;

import static org.assertj.core.api.Assertions.assertThat;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * The immutable click snapshot: it must capture the slot, click type, and viewer at click time, and clone
 * the clicked and cursor items so a later mutation of the live event cannot bleed into a snapshot already
 * handed to an (off-thread) handler.
 */
class ClickContextTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static InventoryClickEvent eventAt(PlayerMock player, Gui gui, int slot) {
        InventoryView view = java.util.Objects.requireNonNull(player.openInventory(gui.getInventory()));
        return new InventoryClickEvent(
                view, InventoryType.SlotType.CONTAINER, slot, ClickType.RIGHT, InventoryAction.PICKUP_HALF);
    }

    @Test
    void capturesSlotAndClickTypeAndViewer() {
        SimpleGui gui = Guis.gui().rows(1).build();
        PlayerMock player = MockBukkit.getMock().addPlayer();
        InventoryClickEvent event = eventAt(player, gui, 4);

        ClickContext context = ClickContext.of(event);

        assertThat(context.slot()).isEqualTo(4);
        assertThat(context.clickType()).isEqualTo(ClickType.RIGHT);
        assertThat(context.viewer()).isEqualTo(player);
    }

    @Test
    void clonesTheClickedItemSoLaterMutationDoesNotLeak() {
        SimpleGui gui = Guis.gui().rows(1).build();
        PlayerMock player = MockBukkit.getMock().addPlayer();
        InventoryClickEvent event = eventAt(player, gui, 0);
        ItemStack live = new ItemStack(Material.DIAMOND, 3);
        event.setCurrentItem(live);

        ClickContext context = ClickContext.of(event);
        live.setAmount(64); // mutate the live stack after the snapshot

        assertThat(context.clickedItem().getAmount()).isEqualTo(3);
        assertThat(context.clickedItem().getType()).isEqualTo(Material.DIAMOND);
    }

    @Test
    void clonesTheCursorItem() {
        SimpleGui gui = Guis.gui().rows(1).build();
        PlayerMock player = MockBukkit.getMock().addPlayer();
        InventoryClickEvent event = eventAt(player, gui, 0);
        event.getView().setCursor(new ItemStack(Material.EMERALD, 2));

        ClickContext context = ClickContext.of(event);

        assertThat(context.cursor().getType()).isEqualTo(Material.EMERALD);
        assertThat(context.cursor().getAmount()).isEqualTo(2);
    }

    @Test
    void anEmptyClickedSlotSnapshotsAsAir() {
        SimpleGui gui = Guis.gui().rows(1).build();
        PlayerMock player = MockBukkit.getMock().addPlayer();
        InventoryClickEvent event = eventAt(player, gui, 0); // nothing in the slot

        ClickContext context = ClickContext.of(event);

        assertThat(context.clickedItem().getType()).isEqualTo(Material.AIR);
    }
}
