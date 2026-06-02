package com.uxplima.uxmlib.gui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import com.uxplima.uxmlib.gui.item.GuiItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Smoke-tests the full wiring of a declarative {@link GuiItem#responding} button through the real
 * {@link GuiListener} dispatch path (listener → {@code AbstractGui.dispatchClick} → {@code GuiClick} →
 * {@code AsyncClick}). With no Scheduler installed the synchronous handler applies inline, so its effect is
 * observable here. This pins the integration seam the unit tests exercise in isolation.
 */
class RespondingClickWiringTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void aRespondingButtonAppliesItsResponsesWhenClickedThroughTheListener() {
        GuiListener listener = new GuiListener();
        SimpleGui gui = Guis.gui().rows(1).build();
        boolean[] ran = {false};
        gui.set(
                0,
                GuiItem.responding(
                        new ItemStack(Material.STONE), ctx -> List.of(GuiResponse.run(() -> ran[0] = true))));
        PlayerMock player = MockBukkit.getMock().addPlayer();
        InventoryView view = java.util.Objects.requireNonNull(player.openInventory(gui.getInventory()));
        InventoryClickEvent event = new InventoryClickEvent(
                view, InventoryType.SlotType.CONTAINER, 0, ClickType.LEFT, InventoryAction.PICKUP_ALL);

        listener.onClick(event);

        assertThat(ran[0]).isTrue();
        assertThat(event.isCancelled()).isTrue(); // the cancel policy still ran
    }
}
