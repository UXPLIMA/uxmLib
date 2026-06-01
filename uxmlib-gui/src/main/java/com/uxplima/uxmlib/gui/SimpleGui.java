package com.uxplima.uxmlib.gui;

import net.kyori.adventure.text.Component;

import com.uxplima.uxmlib.gui.item.GuiItem;

/**
 * A plain single-page menu: a grid of slots the caller fills with {@link GuiItem}s. A chest menu is sized
 * by rows; a {@link GuiType} menu (hopper, dispenser, …) is sized by its type. Created through
 * {@link Guis#gui()} or {@link Guis#typed(GuiType)}.
 */
public final class SimpleGui extends AbstractGui {

    SimpleGui(Component title, int rows) {
        super(title, rows);
    }

    SimpleGui(Component title, GuiType type) {
        super(title, type);
    }
}
