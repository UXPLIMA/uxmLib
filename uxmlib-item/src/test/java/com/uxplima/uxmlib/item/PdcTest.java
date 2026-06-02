package com.uxplima.uxmlib.item;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class PdcTest {

    private static final NamespacedKey COINS = NamespacedKey.minecraft("coins");
    private static final NamespacedKey OWNER = NamespacedKey.minecraft("owner");

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void setThenGetReadsTheStoredValue() {
        ItemStack item = new ItemStack(Material.STONE);

        Items.editPdc(item, pdc -> Pdc.set(pdc, COINS, PersistentDataType.INTEGER, 12));

        Optional<Integer> stored =
                Pdc.get(item.getItemMeta().getPersistentDataContainer(), COINS, PersistentDataType.INTEGER);
        assertThat(stored).contains(12);
    }

    @Test
    void getReturnsEmptyForAMissingKey() {
        ItemStack item = new ItemStack(Material.STONE);

        Optional<Integer> stored =
                Pdc.get(item.getItemMeta().getPersistentDataContainer(), COINS, PersistentDataType.INTEGER);

        assertThat(stored).isEmpty();
    }

    @Test
    void getOrDefaultFallsBackWhenAbsent() {
        ItemStack item = new ItemStack(Material.STONE);

        int coins = Pdc.getOrDefault(
                item.getItemMeta().getPersistentDataContainer(), COINS, PersistentDataType.INTEGER, 99);

        assertThat(coins).isEqualTo(99);
    }

    @Test
    void hasReflectsWhetherAKeyIsPresent() {
        ItemStack item = new ItemStack(Material.STONE);
        assertThat(Pdc.has(item.getItemMeta().getPersistentDataContainer(), COINS, PersistentDataType.INTEGER))
                .isFalse();

        Items.editPdc(item, pdc -> Pdc.set(pdc, COINS, PersistentDataType.INTEGER, 5));

        assertThat(Pdc.has(item.getItemMeta().getPersistentDataContainer(), COINS, PersistentDataType.INTEGER))
                .isTrue();
    }

    @Test
    void storesAndReadsAUuidThroughTheVanillaCodec() {
        ItemStack item = new ItemStack(Material.STONE);
        UUID owner = UUID.randomUUID();

        Items.editPdc(item, pdc -> Pdc.set(pdc, OWNER, UuidArrayType.INSTANCE, owner));

        Optional<UUID> read = Pdc.get(item.getItemMeta().getPersistentDataContainer(), OWNER, UuidArrayType.INSTANCE);
        assertThat(read).contains(owner);
    }

    @Test
    void viewExposesItemKeysWithoutMutation() {
        ItemStack item = new ItemStack(Material.STONE);
        Items.editPdc(item, pdc -> Pdc.set(pdc, COINS, PersistentDataType.INTEGER, 3));

        Pdc.PdcReader reader = Pdc.read(item);

        assertThat(reader.has(COINS, PersistentDataType.INTEGER)).isTrue();
        assertThat(reader.get(COINS, PersistentDataType.INTEGER)).contains(3);
        assertThat(reader.getOrDefault(OWNER.toString(), PersistentDataType.STRING, "none"))
                .isEqualTo("none");
    }
}
