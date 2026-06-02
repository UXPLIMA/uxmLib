package com.uxplima.uxmlib.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import com.uxplima.uxmlib.gui.item.GuiItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/** Covers the bounded click audit log and that the click handler feeds it (#24). */
class GuiClickLogTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static InventoryClickEvent clickAt(SimpleGui gui, org.bukkit.entity.Player player, int slot) {
        var view = java.util.Objects.requireNonNull(player.openInventory(gui.getInventory()));
        return new InventoryClickEvent(
                view, InventoryType.SlotType.CONTAINER, slot, ClickType.LEFT, InventoryAction.PICKUP_ALL);
    }

    @Test
    void ringBufferEvictsTheOldestPastCapacity() {
        SimpleGui gui = Guis.gui().rows(1).build();
        GuiClickLog log = new GuiClickLog(2);
        var player = MockBukkit.getMock().addPlayer();

        log.record(gui, clickAt(gui, player, 0));
        log.record(gui, clickAt(gui, player, 1));
        log.record(gui, clickAt(gui, player, 2));

        assertThat(log.size()).isEqualTo(2);
        var recent = log.recent();
        assertThat(recent.get(0).slot()).isEqualTo(1); // slot 0 evicted
        assertThat(recent.get(1).slot()).isEqualTo(2);
    }

    @Test
    void rejectsANonPositiveCapacity() {
        assertThatThrownBy(() -> new GuiClickLog(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void installedListenerLogsAnAcceptedClick() {
        Guis.install(MockBukkit.createMockPlugin());
        try {
            SimpleGui gui = Guis.gui().rows(1).build();
            gui.set(0, GuiItem.button(new ItemStack(Material.STONE), e -> {}));
            var player = MockBukkit.getMock().addPlayer();

            GuiClickLog log = java.util.Objects.requireNonNull(Guis.clickLog());
            int before = log.size();
            MockBukkit.getMock()
                    .getPluginManager()
                    .callEvent(clickAt(gui, player, 0)); // route through the registered listener

            assertThat(log.size()).isEqualTo(before + 1);
            assertThat(log.recent().get(log.size() - 1).slot()).isEqualTo(0);
        } finally {
            Guis.uninstall();
        }
    }
}
