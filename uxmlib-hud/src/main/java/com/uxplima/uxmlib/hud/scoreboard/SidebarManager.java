package com.uxplima.uxmlib.hud.scoreboard;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import net.kyori.adventure.text.Component;

import org.jspecify.annotations.Nullable;

/**
 * Owns the per-player {@link Sidebar} instances. {@link #create} builds a fresh sidebar on its own native
 * {@link Scoreboard} (so two players never share board state) and shows it, snapshotting the player's prior
 * scoreboard so {@link #remove} can restore it. A second {@code create} for the same player replaces the
 * first. Per-player state lives on this instance — no static map.
 *
 * <p>Quit cleanup is wired by registering a {@link SidebarListener} (it forwards each quitting player's UUID
 * to {@link #forget}); the manager itself stays free of Bukkit event plumbing.
 */
public final class SidebarManager {

    private final ScoreboardManager scoreboards;
    private final Map<UUID, Sidebar> active = new ConcurrentHashMap<>();
    private final Map<UUID, Scoreboard> prior = new ConcurrentHashMap<>();

    public SidebarManager(ScoreboardManager scoreboards) {
        this.scoreboards = Objects.requireNonNull(scoreboards, "scoreboards");
    }

    /** Create, show and track a sidebar with {@code title} for {@code player}, replacing any prior one. */
    public Sidebar create(Player player, Component title) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(title, "title");
        UUID id = player.getUniqueId();
        Sidebar existing = active.remove(id);
        if (existing != null) {
            existing.remove();
        } else {
            prior.put(id, player.getScoreboard());
        }
        Sidebar sidebar = new Sidebar(player, scoreboards.getNewScoreboard(), title);
        active.put(id, sidebar);
        sidebar.show();
        return sidebar;
    }

    /** The sidebar currently shown to {@code player}, or {@code null} if none. */
    public @Nullable Sidebar get(UUID player) {
        Objects.requireNonNull(player, "player");
        return active.get(player);
    }

    /** Remove {@code player}'s sidebar and restore the scoreboard they had before it. */
    public void remove(Player player) {
        Objects.requireNonNull(player, "player");
        UUID id = player.getUniqueId();
        Sidebar sidebar = active.remove(id);
        if (sidebar == null) {
            return;
        }
        sidebar.remove();
        Scoreboard restore = prior.remove(id);
        if (restore != null) {
            player.setScoreboard(restore);
        }
    }

    /** Drop a departed player's sidebar without restoring (they are gone); called by the quit listener. */
    public void forget(UUID player) {
        Objects.requireNonNull(player, "player");
        Sidebar sidebar = active.remove(player);
        if (sidebar != null) {
            sidebar.remove();
        }
        prior.remove(player);
    }

    /** How many players currently have a sidebar. */
    public int count() {
        return active.size();
    }
}
