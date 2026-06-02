package com.uxplima.uxmlib.gui.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.bukkit.Material;

import com.uxplima.uxmlib.config.HoconConfig;
import com.uxplima.uxmlib.gui.PaginatedGui;
import com.uxplima.uxmlib.gui.input.PlayerInput;
import com.uxplima.uxmlib.gui.item.GuiItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;

/** Covers building the config-editor menu: one icon per key, sections vs scalars (#20). */
class ConfigEditorGuiTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static HoconConfig config(Path dir, String body) throws Exception {
        Path file = dir.resolve("config.conf");
        Files.writeString(file, body);
        return HoconConfig.load(file);
    }

    @Test
    void buildsOneIconPerTopLevelKey(@TempDir Path dir) throws Exception {
        HoconConfig config = config(dir, "max-homes = 3\nenabled = true\neconomy { balance = 100 }\n");
        PlayerInput input = new PlayerInput(MockBukkit.createMockPlugin());

        PaginatedGui gui = new ConfigEditorGui(config, input).build();

        // Three keys -> three icons in the content area (slots 0..2), plus nav arrows in the bottom row.
        assertThat(gui.getItem(0)).isNotNull();
        assertThat(gui.getItem(1)).isNotNull();
        assertThat(gui.getItem(2)).isNotNull();
        assertThat(gui.getItem(48)).isNotNull(); // previous arrow
        assertThat(gui.getItem(50)).isNotNull(); // next arrow
    }

    @Test
    void rendersScalarIconsAsPaperAndSectionsAsBook(@TempDir Path dir) throws Exception {
        HoconConfig config = config(dir, "scalar = 5\nsection { child = 1 }\n");
        PlayerInput input = new PlayerInput(MockBukkit.createMockPlugin());
        PaginatedGui gui = new ConfigEditorGui(config, input).build();

        var player = MockBukkit.getMock().addPlayer();
        gui.open(player);

        // Collect the rendered materials of the two content slots; one must be PAPER (scalar), one BOOK (section).
        Material first =
                java.util.Objects.requireNonNull(gui.getInventory().getItem(0)).getType();
        Material second =
                java.util.Objects.requireNonNull(gui.getInventory().getItem(1)).getType();
        assertThat(java.util.List.of(first, second)).containsExactlyInAnyOrder(Material.PAPER, Material.BOOK);
    }

    @Test
    void scalarIconsAreClickableButtons(@TempDir Path dir) throws Exception {
        HoconConfig config = config(dir, "scalar = 5\n");
        PlayerInput input = new PlayerInput(MockBukkit.createMockPlugin());
        PaginatedGui gui = new ConfigEditorGui(config, input).build();

        GuiItem item = gui.getItem(0);
        assertThat(item).isInstanceOf(GuiItem.Static.class);
    }
}
