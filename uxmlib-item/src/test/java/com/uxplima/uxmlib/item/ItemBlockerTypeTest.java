package com.uxplima.uxmlib.item;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class ItemBlockerTypeTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void writeThenReadRoundTripsTheBlockedSet() {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);

        ItemBlockerType.block(item, ItemAction.CRAFT, ItemAction.DROP);

        assertThat(ItemBlockerType.blockedActions(item.getPersistentDataContainer()))
                .containsExactlyInAnyOrder(ItemAction.CRAFT, ItemAction.DROP);
    }

    @Test
    void unmarkedItemBlocksNothing() {
        ItemStack item = new ItemStack(Material.STONE);

        assertThat(ItemBlockerType.blockedActions(item.getPersistentDataContainer()))
                .isEmpty();
        assertThat(ItemBlockerType.isBlocked(item, ItemAction.PLACE)).isFalse();
    }

    @Test
    void isBlockedAnswersPerAction() {
        ItemStack item = new ItemStack(Material.BREAD);
        ItemBlockerType.block(item, Set.of(ItemAction.CONSUME));

        assertThat(ItemBlockerType.isBlocked(item, ItemAction.CONSUME)).isTrue();
        assertThat(ItemBlockerType.isBlocked(item, ItemAction.DROP)).isFalse();
    }

    @Test
    void blockingAnEmptySetClearsTheFlag() {
        ItemStack item = new ItemStack(Material.STONE);
        ItemBlockerType.block(item, ItemAction.PLACE);
        assertThat(ItemBlockerType.isBlocked(item, ItemAction.PLACE)).isTrue();

        ItemBlockerType.block(item, EnumSet.noneOf(ItemAction.class));

        assertThat(ItemBlockerType.blockedActions(item.getPersistentDataContainer()))
                .isEmpty();
    }

    @Test
    void blockingReplacesRatherThanMergesThePreviousSet() {
        ItemStack item = new ItemStack(Material.STONE);
        ItemBlockerType.block(item, ItemAction.PLACE, ItemAction.DROP);

        ItemBlockerType.block(item, ItemAction.CRAFT);

        assertThat(ItemBlockerType.blockedActions(item.getPersistentDataContainer()))
                .containsExactly(ItemAction.CRAFT);
    }

    @Test
    void nullOrEmptyItemIsNeverBlocked() {
        assertThat(ItemBlockerType.isBlocked((ItemStack) null, ItemAction.DROP)).isFalse();
        assertThat(ItemBlockerType.isBlocked(new ItemStack(Material.AIR), ItemAction.DROP))
                .isFalse();
    }

    @Test
    void unknownStoredIdsAreIgnored() {
        ItemStack item = new ItemStack(Material.STONE);
        Items.editPdc(
                item,
                pdc -> pdc.set(
                        ItemBlockerType.KEY, org.bukkit.persistence.PersistentDataType.STRING, "craft,bogus,drop"));

        assertThat(ItemBlockerType.blockedActions(item.getPersistentDataContainer()))
                .containsExactlyInAnyOrder(ItemAction.CRAFT, ItemAction.DROP);
    }

    @Test
    void idsAreStableAndDistinct() {
        assertThat(ItemAction.byId("craft")).contains(ItemAction.CRAFT);
        assertThat(ItemAction.byId("nope")).isEmpty();
        assertThat(java.util.Arrays.stream(ItemAction.values())
                        .map(ItemAction::id)
                        .distinct()
                        .count())
                .isEqualTo(ItemAction.values().length);
    }
}
