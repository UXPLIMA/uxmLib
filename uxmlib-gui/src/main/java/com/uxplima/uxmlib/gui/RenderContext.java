package com.uxplima.uxmlib.gui;

import java.util.Locale;
import java.util.Objects;

import org.bukkit.entity.Player;

/**
 * The per-viewer context handed to a {@link GuiItem} when its icon and action are resolved. It carries
 * who is looking at the menu, which menu and slot, so a dynamic item can render differently for each
 * player — a localized name, the viewer's own head, a value that depends on their permissions. The
 * viewer's {@link #locale()} is exposed so text resolves per player through Adventure.
 */
public record RenderContext(Player viewer, Gui gui, int slot) {

    public RenderContext {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(gui, "gui");
    }

    /** The viewer's client locale, for per-player text resolution. */
    public Locale locale() {
        return viewer.locale();
    }
}
