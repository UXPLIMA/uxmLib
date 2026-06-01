package com.uxplima.uxmlib.hud;

import java.util.Objects;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;

/**
 * Player-list header and footer over Adventure. The header sits above the tab player list and the footer
 * below it; both are delivered with one native call. The header/footer is persistent until changed, but it
 * lives entirely server-to-client, so this helper keeps no per-player state.
 */
public final class Tablist {

    /** Set both the header and footer of {@code player}'s tab list. */
    public void set(Player player, Component header, Component footer) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(header, "header");
        Objects.requireNonNull(footer, "footer");
        player.sendPlayerListHeaderAndFooter(header, footer);
    }

    /** Set only the header, leaving the footer empty. */
    public void header(Player player, Component header) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(header, "header");
        player.sendPlayerListHeaderAndFooter(header, Component.empty());
    }

    /** Set only the footer, leaving the header empty. */
    public void footer(Player player, Component footer) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(footer, "footer");
        player.sendPlayerListHeaderAndFooter(Component.empty(), footer);
    }

    /** Clear both the header and footer. */
    public void clear(Player player) {
        Objects.requireNonNull(player, "player");
        player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
    }
}
