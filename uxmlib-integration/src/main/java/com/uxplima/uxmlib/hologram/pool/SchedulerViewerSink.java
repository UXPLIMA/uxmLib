package com.uxplima.uxmlib.hologram.pool;

import java.util.Objects;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.uxplima.uxmlib.hologram.Hologram;
import com.uxplima.uxmlib.scheduler.Scheduler;

/**
 * The production {@link ViewerSink}: resolves the UUID to an online player and runs the native
 * {@code show}/{@code hide} on the hologram entity's own region thread through the {@link Scheduler}. The
 * pool computes the delta on the global region; bouncing the actual visibility call onto the entity region
 * is what makes the show/hide safe on Folia. A UUID that no longer maps to an online player is skipped.
 */
final class SchedulerViewerSink implements ViewerSink {

    private final Plugin plugin;
    private final Scheduler scheduler;

    SchedulerViewerSink(Plugin plugin, Scheduler scheduler) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    @Override
    public void show(Hologram hologram, UUID viewer) {
        Player player = Bukkit.getPlayer(viewer);
        if (player != null) {
            scheduler.entity(hologram.entity(), () -> hologram.show(plugin, player));
        }
    }

    @Override
    public void hide(Hologram hologram, UUID viewer) {
        Player player = Bukkit.getPlayer(viewer);
        if (player != null) {
            scheduler.entity(hologram.entity(), () -> hologram.hide(plugin, player));
        }
    }
}
