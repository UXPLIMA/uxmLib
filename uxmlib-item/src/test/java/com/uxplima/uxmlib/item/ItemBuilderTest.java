package com.uxplima.uxmlib.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import com.uxplima.uxmlib.text.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class ItemBuilderTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void setsNameAndLoreAsComponents() {
        ItemStack item = ItemBuilder.of(Material.DIAMOND_SWORD)
                .name(Text.mini("<red>Blade"))
                .lore(Text.mini("<gray>line one"), Text.mini("<gray>line two"))
                .build();

        assertThat(Text.plain(item.getItemMeta().displayName())).isEqualTo("Blade");
        assertThat(item.getItemMeta().lore()).hasSize(2);
        assertThat(Text.plain(item.getItemMeta().lore().get(0))).isEqualTo("line one");
    }

    @Test
    void appendsLore() {
        ItemStack item = ItemBuilder.of(Material.PAPER)
                .lore(Component.text("first"))
                .addLore(Component.text("second"))
                .build();

        assertThat(item.getItemMeta().lore()).hasSize(2);
        assertThat(Text.plain(item.getItemMeta().lore().get(1))).isEqualTo("second");
    }

    @Test
    void splitsLoreOnEmbeddedNewlines() {
        ItemStack item = ItemBuilder.of(Material.MAP)
                .lore(Text.mini("<gray>Environment: <white>NETHER</white><newline><gray>Loaded: <white>true</white>"))
                .build();

        assertThat(item.getItemMeta().lore()).hasSize(2);
        assertThat(Text.plain(item.getItemMeta().lore().get(0))).isEqualTo("Environment: NETHER");
        assertThat(Text.plain(item.getItemMeta().lore().get(1))).isEqualTo("Loaded: true");
    }

    @Test
    void defaultsNameAndLoreToUpright() {
        ItemStack item = ItemBuilder.of(Material.MAP)
                .name(Text.mini("<red>Title"))
                .lore(Text.mini("<gray>plain line"), Text.mini("<gray>multi<newline>line"))
                .build();

        assertThat(item.getItemMeta().displayName().decoration(TextDecoration.ITALIC))
                .isEqualTo(TextDecoration.State.FALSE);
        for (Component line : item.getItemMeta().lore()) {
            assertThat(line.decoration(TextDecoration.ITALIC)).isEqualTo(TextDecoration.State.FALSE);
        }
    }

    @Test
    void preservesExplicitItalicOnLore() {
        ItemStack item = ItemBuilder.of(Material.MAP)
                .lore(Text.mini("<i><gray>deliberately italic"))
                .build();

        assertThat(item.getItemMeta().lore().get(0).decoration(TextDecoration.ITALIC))
                .isEqualTo(TextDecoration.State.TRUE);
    }

    @Test
    void clearsNameAndLore() {
        ItemStack named = ItemBuilder.of(Material.DIAMOND_SWORD)
                .name(Component.text("Blade"))
                .lore(Component.text("line"))
                .build();

        ItemStack cleared = ItemBuilder.from(named).clearName().clearLore().build();

        assertThat(cleared.getItemMeta().hasDisplayName()).isFalse();
        assertThat(cleared.getItemMeta().hasLore()).isFalse();
    }

    @Test
    void setsAmountAndRejectsOutOfRange() {
        ItemStack item = ItemBuilder.of(Material.STONE).amount(16).build();
        assertThat(item.getAmount()).isEqualTo(16);

        assertThatThrownBy(() -> ItemBuilder.of(Material.STONE).amount(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void appliesFlagsAndUnbreakable() {
        ItemStack item = ItemBuilder.of(Material.DIAMOND_PICKAXE)
                .flags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES)
                .unbreakable(true)
                .build();

        assertThat(item.getItemMeta().getItemFlags()).contains(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        assertThat(item.getItemMeta().isUnbreakable()).isTrue();
    }

    @Test
    void removesFlagsAndEnchants() {
        ItemStack decorated = ItemBuilder.of(Material.DIAMOND_SWORD)
                .enchant(Enchantment.SHARPNESS, 3)
                .enchant(Enchantment.UNBREAKING, 2)
                .flags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES)
                .build();

        ItemStack stripped = ItemBuilder.from(decorated)
                .removeEnchant(Enchantment.SHARPNESS)
                .removeFlags(ItemFlag.HIDE_ENCHANTS)
                .build();
        assertThat(stripped.getItemMeta().hasEnchant(Enchantment.SHARPNESS)).isFalse();
        assertThat(stripped.getItemMeta().hasEnchant(Enchantment.UNBREAKING)).isTrue();
        assertThat(stripped.getItemMeta().getItemFlags()).containsExactly(ItemFlag.HIDE_ATTRIBUTES);

        ItemStack disenchanted = ItemBuilder.from(decorated).clearEnchants().build();
        assertThat(disenchanted.getItemMeta().hasEnchants()).isFalse();
    }

    @Test
    void appliesDamageToADamageableItem() {
        ItemStack item = ItemBuilder.of(Material.IRON_AXE).damage(50).build();

        assertThat(item.getItemMeta()).isInstanceOf(Damageable.class);
        assertThat(((Damageable) item.getItemMeta()).getDamage()).isEqualTo(50);
    }

    @Test
    void rejectsAirAndNegativeDamage() {
        assertThatThrownBy(() -> ItemBuilder.of(Material.AIR)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ItemBuilder.of(Material.IRON_AXE).damage(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsSkullOnANonHead() {
        assertThatThrownBy(() -> ItemBuilder.of(Material.STONE).skull(SkullData.ofName("Notch")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void writesPersistentData() {
        NamespacedKey key = NamespacedKey.fromString("uxmlib:test");
        ItemStack item = ItemBuilder.of(Material.STONE)
                .editPersistentData(pdc -> pdc.set(key, PersistentDataType.STRING, "value"))
                .build();

        String stored = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        assertThat(stored).isEqualTo("value");
    }

    @Test
    void glowSetsTheGlintOverrideWithoutAnEnchant() {
        ItemStack item = ItemBuilder.of(Material.STONE).glow(true).build();

        assertThat(item.getItemMeta().hasEnchantmentGlintOverride()).isTrue();
        assertThat(item.getItemMeta().getEnchantmentGlintOverride()).isTrue();
        assertThat(item.getItemMeta().hasEnchants()).isFalse();
    }

    @Test
    @SuppressWarnings("deprecation") // getCustomModelData() is the int read-back; the value round-trips
    void setsCustomModelData() {
        ItemStack item = ItemBuilder.of(Material.STONE).customModelData(42).build();

        assertThat(item.getItemMeta().hasCustomModelData()).isTrue();
        assertThat(item.getItemMeta().getCustomModelData()).isEqualTo(42);
    }

    @Test
    void setsMaxStackSizeAndRejectsOutOfRange() {
        ItemStack item = ItemBuilder.of(Material.DIAMOND_SWORD).maxStackSize(16).build();
        assertThat(item.getItemMeta().getMaxStackSize()).isEqualTo(16);

        assertThatThrownBy(() -> ItemBuilder.of(Material.STONE).maxStackSize(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ItemBuilder.of(Material.STONE).maxStackSize(100))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setsRarity() {
        ItemStack item = ItemBuilder.of(Material.STONE).rarity(ItemRarity.EPIC).build();

        assertThat(item.getItemMeta().getRarity()).isEqualTo(ItemRarity.EPIC);
    }

    @Test
    void nameOnAnEpicMaterialIsNotTintedByRarity() {
        // A nether star is epic-rarity by default; its name would otherwise render light-purple. Setting a
        // custom name must pin the rarity to COMMON so the name keeps the colour the component carries.
        ItemStack item = ItemBuilder.of(Material.NETHER_STAR)
                .name(Text.mini("<color:#45cdf9>Your Homes</color>"))
                .build();

        assertThat(item.getItemMeta().getRarity()).isEqualTo(ItemRarity.COMMON);
        assertThat(item.getItemMeta().displayName().color()).isEqualTo(TextColor.color(0x45cdf9));
    }

    @Test
    void setsItemModelWithoutThrowing() {
        // MockBukkit's ItemMeta does not persist item_model, so we can only assert the wiring is valid;
        // the setItemModel signature itself is verified against the real paper-api 1.21.11 jar.
        NamespacedKey model = NamespacedKey.fromString("uxmlib:custom");
        assertThatCode(() -> ItemBuilder.of(Material.STONE).itemModel(model).build())
                .doesNotThrowAnyException();
    }

    @Test
    void buildReturnsIndependentCopies() {
        ItemBuilder builder = ItemBuilder.of(Material.STONE).amount(1);
        ItemStack first = builder.build();
        builder.amount(32);
        ItemStack second = builder.build();

        assertThat(first.getAmount()).isEqualTo(1);
        assertThat(second.getAmount()).isEqualTo(32);
    }
}
