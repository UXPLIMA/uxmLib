package com.uxplima.uxmlib.hud.scoreboard;

import java.util.Objects;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Drops a quitting player's sidebar so the manager does not leak its UUID or its native scoreboard. Owned
 * and registered by the consumer alongside its {@link SidebarManager}; it simply forwards the departing
 * player's UUID to {@link SidebarManager#forget}.
 */
public final class SidebarListener implements Listener {

    private final SidebarManager manager;

    public SidebarListener(SidebarManager manager) {
        this.manager = Objects.requireNonNull(manager, "manager");
    }

    @EventHandler
    void onQuit(PlayerQuitEvent event) {
        manager.forget(event.getPlayer().getUniqueId());
    }
}
