package com.uxplima.uxmlib.gui.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import com.uxplima.uxmlib.text.message.MessageCatalog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/** Covers the mergeable icon spec (#23) and its lang-key resolution (#21). */
class IconOptionsTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static String name(ItemStack item) {
        return PlainTextComponentSerializer.plainText()
                .serialize(java.util.Objects.requireNonNull(
                        java.util.Objects.requireNonNull(item.getItemMeta()).displayName()));
    }

    @Test
    void combineOverlaysAnOverrideOntoABase() {
        IconOptions base = new IconOptions(Material.STONE, "<gray>Base", List.of("<gray>line"), 1, null, null);
        IconOptions override = new IconOptions(Material.EMERALD, "<green>Override", null, null, null, null);

        IconOptions merged = IconOptions.combine(base, override);
        ItemStack item = merged.toItem(null, Locale.ENGLISH);

        assertThat(item.getType()).isEqualTo(Material.EMERALD); // override material wins
        assertThat(name(item)).isEqualTo("Override"); // override name wins
        // The override left lore unspecified, so the base lore carries over.
        assertThat(java.util.Objects.requireNonNull(item.getItemMeta()).lore()).hasSize(1);
    }

    @Test
    void combineKeepsBaseFieldsTheOverrideLeavesUnset() {
        IconOptions base = new IconOptions(Material.DIAMOND, "<aqua>Gem", null, 3, null, null);
        IconOptions merged = IconOptions.combine(base, IconOptions.empty());

        ItemStack item = merged.toItem(null, Locale.ENGLISH);
        assertThat(item.getType()).isEqualTo(Material.DIAMOND);
        assertThat(name(item)).isEqualTo("Gem");
        assertThat(item.getAmount()).isEqualTo(3);
    }

    @Test
    void resolvesNameFromTheLangCatalogWhenAKeyIsGiven() {
        MessageCatalog catalog =
                new MessageCatalog(Map.of(Locale.ENGLISH, Map.of("menu.close", "<red>Close the menu")), Locale.ENGLISH);
        IconOptions options = new IconOptions(Material.BARRIER, null, null, null, "menu.close", null);

        ItemStack item = options.toItem(catalog, Locale.ENGLISH);

        assertThat(name(item)).isEqualTo("Close the menu");
    }

    @Test
    void inlineNameStillRendersWithNoCatalog() {
        IconOptions options = new IconOptions(Material.PAPER, "<white>Plain", null, null, null, null);
        ItemStack item = options.toItem(null, Locale.ENGLISH);
        assertThat(name(item)).isEqualTo("Plain");
    }
}
