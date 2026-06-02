package com.uxplima.uxmlib.gui.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Material;

import com.uxplima.uxmlib.gui.SimpleGui;
import com.uxplima.uxmlib.gui.item.GuiItem;
import com.uxplima.uxmlib.text.message.MessageCatalog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

/** Covers the self-migrating menu config (#22) and lang-key icon names through MenuConfig (#21). */
class MenuConfigMigrationTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static CommentedConfigurationNode parse(String hocon) throws Exception {
        return HoconConfigurationLoader.builder()
                .source(() -> new java.io.BufferedReader(new java.io.StringReader(hocon)))
                .build()
                .load();
    }

    @Test
    void rewritesLegacyKeysToCurrentNamesInPlace() throws Exception {
        // 'slots' is the old name for 'mask'; 'material-type' the old name for 'material' inside an item.
        String hocon =
                """
                title = "<gold>Shop"
                size = 1
                slots = [ "D        " ]
                items {
                  D { material-type = "DIAMOND", display-name = "<aqua>Gem" }
                }
                """;
        CommentedConfigurationNode node = parse(hocon);

        MenuConfig.migrate(node);

        assertThat(node.node("mask").virtual()).isFalse();
        assertThat(node.node("slots").virtual()).isTrue(); // legacy key dropped
        assertThat(node.node("rows").getInt()).isEqualTo(1); // size -> rows
        assertThat(node.node("items", "D", "material").getString()).isEqualTo("DIAMOND");
        assertThat(node.node("items", "D", "material-type").virtual()).isTrue();
    }

    @Test
    void loadAppliesMigrationSoAnOldConfigStillBuilds() throws Exception {
        String hocon =
                """
                size = 1
                slots = [ "D        " ]
                items {
                  D { material-type = "DIAMOND" }
                }
                """;
        SimpleGui gui = MenuConfig.load(parse(hocon), new MenuActions());

        GuiItem item = gui.getItem(0);
        assertThat(item).isInstanceOf(GuiItem.Static.class);
        assertThat(((GuiItem.Static) java.util.Objects.requireNonNull(item))
                        .item()
                        .getType())
                .isEqualTo(Material.DIAMOND);
    }

    @Test
    void keepsACurrentKeyWhenBothAreSet() throws Exception {
        // If a config has both legacy and current keys, the current value must not be clobbered.
        String hocon =
                """
                rows = 1
                size = 6
                mask = [ "D        " ]
                items {
                  D { material = "DIAMOND" }
                }
                """;
        CommentedConfigurationNode node = parse(hocon);

        MenuConfig.migrate(node);

        assertThat(node.node("rows").getInt()).isEqualTo(1); // current 'rows' wins over legacy 'size'
    }

    @Test
    void resolvesAnIconNameFromTheLangCatalog() throws Exception {
        String hocon =
                """
                rows = 1
                mask = [ "C        " ]
                items {
                  C { material = "BARRIER", name-key = "menu.close" }
                }
                """;
        MessageCatalog catalog =
                new MessageCatalog(Map.of(Locale.ENGLISH, Map.of("menu.close", "<red>Close")), Locale.ENGLISH);

        SimpleGui gui = MenuConfig.load(parse(hocon), new MenuActions(), new MenuConditions(), catalog);

        var icon = ((GuiItem.Static) java.util.Objects.requireNonNull(gui.getItem(0))).item();
        String name = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(java.util.Objects.requireNonNull(
                        java.util.Objects.requireNonNull(icon.getItemMeta()).displayName()));
        assertThat(name).isEqualTo("Close");
        assertThat(List.of(icon.getType())).containsExactly(Material.BARRIER);
    }
}
