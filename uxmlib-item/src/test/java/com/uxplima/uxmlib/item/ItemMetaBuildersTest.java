package com.uxplima.uxmlib.item;

import static org.assertj.core.api.Assertions.assertThat;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class ItemMetaBuildersTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void dyesLeatherArmour() {
        ItemStack boots = ItemBuilder.of(Material.LEATHER_BOOTS)
                .leatherColor(Color.fromRGB(0x00FF00))
                .build();

        assertThat(boots.getItemMeta()).isInstanceOf(LeatherArmorMeta.class);
        assertThat(((LeatherArmorMeta) boots.getItemMeta()).getColor()).isEqualTo(Color.fromRGB(0x00FF00));
    }

    @Test
    void appliesFireworkEffectAndPower() {
        FireworkEffect effect = FireworkEffect.builder()
                .withColor(Color.AQUA)
                .with(FireworkEffect.Type.BALL_LARGE)
                .build();
        ItemStack rocket = ItemBuilder.of(Material.FIREWORK_ROCKET)
                .fireworkEffect(effect)
                .fireworkPower(2)
                .build();

        FireworkMeta meta = (FireworkMeta) rocket.getItemMeta();
        assertThat(meta.getPower()).isEqualTo(2);
        assertThat(meta.getEffects()).containsExactly(effect);
    }

    @Test
    void typedMetaIsANoOpOnTheWrongItem() {
        // Applying a leather colour to stone must not throw and must leave a normal item.
        ItemStack stone = ItemBuilder.of(Material.STONE).leatherColor(Color.RED).build();
        assertThat(stone.getType()).isEqualTo(Material.STONE);
        assertThat(stone.getItemMeta()).isNotInstanceOf(LeatherArmorMeta.class);
    }

    @Test
    void rejectsOutOfRangeFireworkPower() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> ItemBuilder.of(Material.FIREWORK_ROCKET).fireworkPower(200))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
