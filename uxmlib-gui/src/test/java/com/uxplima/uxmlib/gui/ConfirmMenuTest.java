package com.uxplima.uxmlib.gui;

import static org.assertj.core.api.Assertions.assertThat;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;

import net.kyori.adventure.text.Component;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/** Covers the yes/no confirmation menu wiring: confirm fires true, cancel fires false. */
class ConfirmMenuTest {

    private static final int CONFIRM_SLOT = 11;
    private static final int CANCEL_SLOT = 15;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static InventoryClickEvent clickAt(SimpleGui gui, int slot) {
        Player player = MockBukkit.getMock().addPlayer();
        InventoryView view = java.util.Objects.requireNonNull(player.openInventory(gui.getInventory()));
        return new InventoryClickEvent(
                view, InventoryType.SlotType.CONTAINER, slot, ClickType.LEFT, InventoryAction.PICKUP_ALL);
    }

    @Test
    void confirmButtonFiresTrue() {
        Boolean[] result = {null};
        ConfirmMenu menu = ConfirmMenu.of(Component.text("Sure?"), confirmed -> result[0] = confirmed);

        InventoryClickEvent event = clickAt(menu.gui(), CONFIRM_SLOT);
        menu.gui().handleClick(event);

        assertThat(result[0]).isTrue();
    }

    @Test
    void cancelButtonFiresFalse() {
        Boolean[] result = {null};
        ConfirmMenu menu = ConfirmMenu.of(Component.text("Sure?"), confirmed -> result[0] = confirmed);

        InventoryClickEvent event = clickAt(menu.gui(), CANCEL_SLOT);
        menu.gui().handleClick(event);

        assertThat(result[0]).isFalse();
    }

    @Test
    void runnableOverloadRoutesToTheRightBranch() {
        boolean[] confirmed = {false};
        boolean[] cancelled = {false};
        ConfirmMenu menu =
                ConfirmMenu.of(Component.text("Sure?"), () -> confirmed[0] = true, () -> cancelled[0] = true);

        menu.gui().handleClick(clickAt(menu.gui(), CANCEL_SLOT));
        assertThat(cancelled[0]).isTrue();
        assertThat(confirmed[0]).isFalse();
    }

    @Test
    void confirmClickCancelsTheEventSoNoItemLeaks() {
        ConfirmMenu menu = ConfirmMenu.of(Component.text("Sure?"), confirmed -> {});
        InventoryClickEvent event = clickAt(menu.gui(), CONFIRM_SLOT);

        menu.gui().handleClick(event);

        assertThat(event.isCancelled()).isTrue();
    }

    @Test
    void customIconsBuildAndPlaceButtons() {
        ConfirmMenu menu = ConfirmMenu.builder(Component.text("Sure?"))
                .confirmIcon(new org.bukkit.inventory.ItemStack(org.bukkit.Material.EMERALD))
                .cancelIcon(new org.bukkit.inventory.ItemStack(org.bukkit.Material.BARRIER))
                .onResult(confirmed -> {})
                .build();

        assertThat(menu.gui().getItem(CONFIRM_SLOT)).isNotNull();
        assertThat(menu.gui().getItem(CANCEL_SLOT)).isNotNull();
        assertThat(menu.gui().size()).isEqualTo(27);
    }
}
