package com.uxplima.uxmlib.item;

import static org.assertj.core.api.Assertions.assertThat;

import org.bukkit.Material;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Smoke test for the Bukkit wiring: register {@link ItemActionListener} on a mock server, fire a consume event
 * with a flagged item, and confirm the listener cancels it while an unflagged item passes. The
 * predicate/PDC logic is unit-tested in {@link ItemBlockerTypeTest}; this only asserts the event-to-predicate
 * bridge holds together against a real (mock) server.
 */
class ItemActionListenerSmokeTest {

    private ServerMock server;
    private Plugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        server.getPluginManager().registerEvents(new ItemActionListener(), plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void consumeOfAFlaggedItemIsCancelled() {
        PlayerMock player = server.addPlayer();
        ItemStack bread = new ItemStack(Material.BREAD);
        ItemBlockerType.block(bread, ItemAction.CONSUME);

        PlayerItemConsumeEvent event = new PlayerItemConsumeEvent(player, bread, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        assertThat(event.isCancelled()).isTrue();
    }

    @Test
    void consumeOfAnUnflaggedItemIsAllowed() {
        PlayerMock player = server.addPlayer();
        ItemStack bread = new ItemStack(Material.BREAD);

        PlayerItemConsumeEvent event = new PlayerItemConsumeEvent(player, bread, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        assertThat(event.isCancelled()).isFalse();
    }

    @Test
    void anItemFlaggedForADifferentActionStillConsumes() {
        PlayerMock player = server.addPlayer();
        ItemStack bread = new ItemStack(Material.BREAD);
        ItemBlockerType.block(bread, ItemAction.DROP);

        PlayerItemConsumeEvent event = new PlayerItemConsumeEvent(player, bread, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        assertThat(event.isCancelled()).isFalse();
    }
}
