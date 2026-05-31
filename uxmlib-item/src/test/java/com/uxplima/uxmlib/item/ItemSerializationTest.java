package com.uxplima.uxmlib.item;

import static org.assertj.core.api.Assertions.assertThat;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;

import com.uxplima.uxmlib.text.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class ItemSerializationTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void roundTripsThroughBytes() {
        ItemStack original = ItemBuilder.of(Material.DIAMOND_SWORD)
                .name(Text.mini("<red>Blade"))
                .amount(3)
                .build();

        ItemStack restored = ItemSerialization.fromBytes(ItemSerialization.toBytes(original));

        assertThat(restored).isEqualTo(original);
        assertThat(restored.getAmount()).isEqualTo(3);
    }

    @Test
    void roundTripsThroughBase64() {
        ItemStack original = ItemBuilder.of(Material.PAPER)
                .name(Component.text("Note"))
                .lore(Component.text("line one"))
                .build();

        String encoded = ItemSerialization.toBase64(original);
        ItemStack restored = ItemSerialization.fromBase64(encoded);

        assertThat(restored).isEqualTo(original);
    }
}
