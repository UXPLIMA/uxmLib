package com.uxplima.uxmlib.hologram;

import org.bukkit.entity.Player;

/** A click on a clickable hologram: who clicked and which button. */
public record HologramClick(Player player, Type type) {

    /** Which mouse button produced the click. */
    public enum Type {
        LEFT,
        RIGHT
    }
}
