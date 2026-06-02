package com.uxplima.uxmlib.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * Pins the craft-block ordering: {@link ItemActionListener#onPrepareCraft} must null a flagged recipe result
 * even when an earlier (default-priority) listener has just produced one, which only holds because the handler
 * runs at {@link EventPriority#HIGHEST}. {@link PrepareItemCraftEvent} has no MockBukkit inventory mock, so the
 * crafting inventory is a Mockito stub whose result is tracked through a real event dispatch.
 */
class ItemActionCraftSmokeTest {

    private ServerMock server;
    private Plugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void aLaterListenerCannotReviveAResultBlockedAtHighest() {
        AtomicReference<ItemStack> result = new AtomicReference<>(null);
        ItemStack flagged = new ItemStack(Material.DIAMOND);
        ItemBlockerType.block(flagged, ItemAction.CRAFT);
        CraftingInventory inventory = craftingInventoryWith(flagged, result);

        // A recipe-completing plugin at HIGH priority runs before our HIGHEST handler and (re)sets a result;
        // were our handler at the default NORMAL priority it would run first and the HIGH listener would win.
        server.getPluginManager().registerEvents(new RecipeCompleter(), plugin);
        server.getPluginManager().registerEvents(new ItemActionListener(), plugin);

        PrepareItemCraftEvent event = new PrepareItemCraftEvent(inventory, mock(InventoryView.class), false);
        server.getPluginManager().callEvent(event);

        // The HIGHEST handler nulled the blocked craft last; the HIGH completer cannot un-block it.
        verify(inventory, atLeastOnce()).setResult(null);
        assertThat(result.get()).isNull();
    }

    @Test
    void anUnflaggedMatrixKeepsTheCompletedResult() {
        AtomicReference<ItemStack> result = new AtomicReference<>(null);
        CraftingInventory inventory = craftingInventoryWith(new ItemStack(Material.STICK), result);

        server.getPluginManager().registerEvents(new RecipeCompleter(), plugin);
        server.getPluginManager().registerEvents(new ItemActionListener(), plugin);

        PrepareItemCraftEvent event = new PrepareItemCraftEvent(inventory, mock(InventoryView.class), false);
        server.getPluginManager().callEvent(event);

        assertThat(result.get()).isEqualTo(new ItemStack(Material.EMERALD));
    }

    private static CraftingInventory craftingInventoryWith(ItemStack matrixItem, AtomicReference<ItemStack> result) {
        CraftingInventory inventory = mock(CraftingInventory.class);
        when(inventory.getMatrix()).thenReturn(new ItemStack[] {matrixItem});
        doAnswer(call -> {
                    result.set(call.getArgument(0));
                    return null;
                })
                .when(inventory)
                .setResult(org.mockito.ArgumentMatchers.any());
        return inventory;
    }

    // Stands in for a plugin that completes the recipe at HIGH priority, i.e. after a default-priority block
    // would have run but before our HIGHEST one.
    private static final class RecipeCompleter implements Listener {
        @EventHandler(priority = EventPriority.HIGH)
        @SuppressWarnings("UnusedMethod") // invoked reflectively by the Bukkit event bus, not from Java
        public void onPrepareCraft(PrepareItemCraftEvent event) {
            event.getInventory().setResult(new ItemStack(Material.EMERALD));
        }
    }
}
