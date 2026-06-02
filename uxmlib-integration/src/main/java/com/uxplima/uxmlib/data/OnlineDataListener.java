package com.uxplima.uxmlib.data;

import java.util.Objects;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * The single Bukkit listener behind {@link OnlineDataManager}: a join triggers the manager's load-on-join, a
 * quit triggers its save-and-evict. Owning exactly one listener here is what lets a consumer wire the whole
 * lifecycle with one {@link OnlineDataManager#installListener} call.
 *
 * @param <V> the per-player value type the manager caches
 */
final class OnlineDataListener<V> implements Listener {

    private final OnlineDataManager<V> manager;

    OnlineDataListener(OnlineDataManager<V> manager) {
        this.manager = Objects.requireNonNull(manager, "manager");
    }

    @EventHandler
    void onJoin(PlayerJoinEvent event) {
        manager.handleJoin(event.getPlayer().getUniqueId());
    }

    @EventHandler
    void onQuit(PlayerQuitEvent event) {
        manager.handleQuit(event.getPlayer().getUniqueId());
    }
}
