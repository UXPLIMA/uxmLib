package com.uxplima.uxmlib.gui;

import static org.assertj.core.api.Assertions.assertThat;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/** Covers vertical/horizontal scrolling, offset clamping, and the rendered window. */
class ScrollingGuiTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static ScrollingGui verticalWith(int itemCount, int rows) {
        ScrollingGui gui = Guis.scrolling(ScrollType.VERTICAL).rows(rows).build();
        for (int i = 0; i < itemCount; i++) {
            gui.addScrollItem(GuiItem.display(new ItemStack(Material.STONE)));
        }
        return gui;
    }

    @Test
    void startsAtOffsetZero() {
        ScrollingGui gui = verticalWith(54, 3); // 3 rows window, 6 rows of content
        assertThat(gui.offset()).isZero();
        assertThat(gui.maxOffset()).isEqualTo(3); // 6 content rows - 3 visible = 3
    }

    @Test
    void scrollNextAdvancesOneRow() {
        ScrollingGui gui = verticalWith(54, 3);
        gui.scrollNext();
        assertThat(gui.offset()).isEqualTo(1);
    }

    @Test
    void scrollClampsAtBothEnds() {
        ScrollingGui gui = verticalWith(54, 3);
        gui.scrollPrevious(); // already at 0
        assertThat(gui.offset()).isZero();
        for (int i = 0; i < 10; i++) {
            gui.scrollNext(); // can only reach maxOffset
        }
        assertThat(gui.offset()).isEqualTo(gui.maxOffset());
    }

    @Test
    void rendersTheWindowStartingAtTheOffset() {
        ScrollingGui gui = verticalWith(54, 3);
        gui.render();
        // First visible item is scroll item 0 at slot 0.
        assertThat(gui.getItem(0)).isNotNull();

        gui.scrollNext(); // window now starts one row (9 items) down
        // Slot 0 now shows scroll item 9; still present, but the window has shifted.
        assertThat(gui.getItem(0)).isNotNull();
        assertThat(gui.offset()).isEqualTo(1);
    }

    @Test
    void shortListNeverScrolls() {
        ScrollingGui gui = verticalWith(5, 3); // fits in the first row
        assertThat(gui.maxOffset()).isZero();
        gui.scrollNext();
        assertThat(gui.offset()).isZero();
    }

    @Test
    void horizontalScrollUsesColumns() {
        ScrollingGui gui = Guis.scrolling(ScrollType.HORIZONTAL).rows(3).build();
        for (int i = 0; i < 60; i++) {
            gui.addScrollItem(GuiItem.display(new ItemStack(Material.STONE)));
        }
        // 60 items / 3 rows per column = 20 columns; window is 9 wide, so maxOffset = 11.
        assertThat(gui.maxOffset()).isEqualTo(11);
        gui.scrollNext();
        assertThat(gui.offset()).isEqualTo(1);
    }
}
