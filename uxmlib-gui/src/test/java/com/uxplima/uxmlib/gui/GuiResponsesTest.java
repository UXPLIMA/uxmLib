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
 * Applies declarative responses in order against a real menu and viewer. The point of side-effects-as-data
 * is that this application step is the only place Bukkit is touched, so it is exercised directly here while
 * handlers stay pure.
 */
class GuiResponsesTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static InventoryClickEvent clickOn(PlayerMock player, Gui gui) {
        InventoryView view = java.util.Objects.requireNonNull(player.openInventory(gui.getInventory()));
        return new InventoryClickEvent(
                view, InventoryType.SlotType.CONTAINER, 0, ClickType.LEFT, InventoryAction.PICKUP_ALL);
    }

    @Test
    void runResponseRunsItsTask() {
        SimpleGui gui = Guis.gui().rows(1).build();
        PlayerMock player = MockBukkit.getMock().addPlayer();
        InventoryClickEvent event = clickOn(player, gui);
        boolean[] ran = {false};

        GuiResponses.apply(List.of(GuiResponse.run(() -> ran[0] = true)), gui, event);

        assertThat(ran[0]).isTrue();
    }

    @Test
    void appliesResponsesInOrder() {
        SimpleGui gui = Guis.gui().rows(1).build();
        PlayerMock player = MockBukkit.getMock().addPlayer();
        InventoryClickEvent event = clickOn(player, gui);
        List<Integer> order = new java.util.ArrayList<>();

        GuiResponses.apply(
                List.of(GuiResponse.run(() -> order.add(1)), GuiResponse.run(() -> order.add(2))), gui, event);

        assertThat(order).containsExactly(1, 2);
    }

    @Test
    void updateItemPlacesTheItemInTheMenu() {
        SimpleGui gui = Guis.gui().rows(1).build();
        PlayerMock player = MockBukkit.getMock().addPlayer();
        InventoryClickEvent event = clickOn(player, gui);
        GuiItem placed = GuiItem.display(new ItemStack(Material.BEACON));

        GuiResponses.apply(List.of(GuiResponse.updateItem(3, placed)), gui, event);

        assertThat(gui.getItem(3)).isSameAs(placed);
    }

    @Test
    void replaceCursorSetsTheEventCursor() {
        SimpleGui gui = Guis.gui().rows(1).build();
        PlayerMock player = MockBukkit.getMock().addPlayer();
        InventoryClickEvent event = clickOn(player, gui);

        GuiResponses.apply(List.of(GuiResponse.replaceCursor(new ItemStack(Material.GOLD_INGOT, 5))), gui, event);

        assertThat(event.getCursor().getType()).isEqualTo(Material.GOLD_INGOT);
        assertThat(event.getCursor().getAmount()).isEqualTo(5);
    }

    @Test
    void nothingIsANoOp() {
        SimpleGui gui = Guis.gui().rows(1).build();
        PlayerMock player = MockBukkit.getMock().addPlayer();
        InventoryClickEvent event = clickOn(player, gui);

        // Must not throw and must leave the menu untouched.
        GuiResponses.apply(List.of(GuiResponse.nothing()), gui, event);

        assertThat(gui.getItem(0)).isNull();
    }
}
