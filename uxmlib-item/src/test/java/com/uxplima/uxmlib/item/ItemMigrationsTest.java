package com.uxplima.uxmlib.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.OptionalInt;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class ItemMigrationsTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void needsMigrationIsTrueWhenStampedVersionIsBelowTheServer() {
        // A blob stamped one data version behind the running server is older and must be migrated.
        byte[] aged = agedBy(ItemBuilder.of(Material.STONE).build(), 1);

        assertThat(ItemMigrations.needsMigration(aged)).isTrue();
    }

    @Test
    void needsMigrationIsFalseWhenStampedVersionMatchesTheServer() {
        // A blob this server just wrote carries the current data version, so nothing to migrate.
        byte[] fresh = ItemSerialization.toBytes(ItemBuilder.of(Material.STONE).build());

        assertThat(ItemMigrations.needsMigration(fresh)).isFalse();
    }

    @Test
    void needsMigrationIsTrueForALegacyHeaderlessBlob() {
        // No stamp at all predates the header era, so its true age is unknown and we migrate defensively.
        byte[] legacy = ItemBuilder.of(Material.STONE).build().serializeAsBytes();

        assertThat(ItemMigrations.needsMigration(legacy)).isTrue();
    }

    @Test
    void migrateRoundTripsAnAgedBlobBackToAUsableItem() {
        byte[] aged = agedBy(ItemBuilder.of(Material.DIAMOND).amount(4).build(), 1);

        byte[] migrated = ItemMigrations.migrate(aged);
        ItemStack restored = ItemSerialization.fromBytes(migrated);

        assertThat(restored.getType()).isEqualTo(Material.DIAMOND);
        assertThat(restored.getAmount()).isEqualTo(4);
    }

    @Test
    void migrateRestampsTheBlobAtTheCurrentDataVersion() {
        byte[] aged = agedBy(ItemBuilder.of(Material.PAPER).build(), 5);

        byte[] migrated = ItemMigrations.migrate(aged);

        // The re-stamp matches the version a freshly written blob carries on this server.
        OptionalInt current = ItemSerialization.dataVersionOf(
                ItemSerialization.toBytes(ItemBuilder.of(Material.PAPER).build()));
        assertThat(ItemSerialization.dataVersionOf(migrated)).isEqualTo(current);
        assertThat(ItemMigrations.needsMigration(migrated)).isFalse();
    }

    @Test
    void migrateItemReturnsTheUpgradedStack() {
        byte[] aged = agedBy(ItemBuilder.of(Material.GOLD_INGOT).amount(2).build(), 1);

        ItemStack migrated = ItemMigrations.migrateItem(aged);

        assertThat(migrated.getType()).isEqualTo(Material.GOLD_INGOT);
        assertThat(migrated.getAmount()).isEqualTo(2);
    }

    @Test
    void migrateAlsoUpgradesALegacyHeaderlessBlob() {
        byte[] legacy = ItemBuilder.of(Material.EMERALD).amount(3).build().serializeAsBytes();

        byte[] migrated = ItemMigrations.migrate(legacy);

        assertThat(ItemSerialization.dataVersionOf(migrated)).isPresent();
        assertThat(ItemSerialization.fromBytes(migrated).getType()).isEqualTo(Material.EMERALD);
    }

    @Test
    void migratingAnAlreadyCurrentBlobIsAnIdentityOnTheItem() {
        ItemStack original = ItemBuilder.of(Material.STONE).amount(6).build();
        byte[] current = ItemSerialization.toBytes(original);

        ItemStack restored = ItemSerialization.fromBytes(ItemMigrations.migrate(current));

        assertThat(restored.getType()).isEqualTo(Material.STONE);
        assertThat(restored.getAmount()).isEqualTo(6);
    }

    @Test
    void rejectsBytesThatAreNotASerializedItem() {
        assertThatThrownBy(() -> ItemMigrations.migrate(new byte[] {1, 2, 3, 4}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // Serialize an item, then rewind the stamped data version by {@code gap} so it looks like an older blob.
    // Reading the current version from the header avoids the deprecated Bukkit#getUnsafe in test code.
    private static byte[] agedBy(ItemStack item, int gap) {
        byte[] fresh = ItemSerialization.toBytes(item);
        int olderVersion = ItemSerialization.dataVersionOf(fresh).orElseThrow() - gap;
        byte[] copy = fresh.clone();
        int offset = "UXMI".length() + 1;
        copy[offset] = (byte) (olderVersion >>> 24);
        copy[offset + 1] = (byte) (olderVersion >>> 16);
        copy[offset + 2] = (byte) (olderVersion >>> 8);
        copy[offset + 3] = (byte) olderVersion;
        return copy;
    }
}
