package com.uxplima.uxmlib.hologram.widget;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.uxplima.uxmlib.hologram.Hologram;
import com.uxplima.uxmlib.scheduler.Scheduler;

/**
 * The production {@link PagePresenter}: each page is one restricted {@link Hologram} (spawned and registered
 * by the consumer, hidden by default) and this maps a page index to it. A {@code show}/{@code hide} resolves
 * the UUID to an online player and runs the native per-viewer call on the page entity's own region thread
 * through the {@link Scheduler}, the same Folia-safe pattern the pool's {@code SchedulerViewerSink} uses. A
 * UUID that no longer maps to an online player is skipped.
 *
 * <p>The page holograms must already be {@code restrictToViewers()} so they are invisible until a viewer is
 * shown a page. Their lifetime (spawn / remove) stays the consumer's or the {@code HologramManager}'s job.
 */
public final class DisplayPagePresenter implements PagePresenter {

    private final List<Hologram> pages;
    private final Plugin plugin;
    private final Scheduler scheduler;

    public DisplayPagePresenter(List<Hologram> pages, Plugin plugin, Scheduler scheduler) {
        Objects.requireNonNull(pages, "pages");
        if (pages.isEmpty()) {
            throw new IllegalArgumentException("a paged hologram needs at least one page");
        }
        this.pages = List.copyOf(pages);
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    /** How many page holograms this presenter drives. */
    public int pageCount() {
        return pages.size();
    }

    @Override
    public void show(int page, UUID viewer) {
        Hologram hologram = pageAt(page);
        Player player = Bukkit.getPlayer(Objects.requireNonNull(viewer, "viewer"));
        if (player != null) {
            scheduler.entity(hologram.entity(), () -> hologram.show(plugin, player));
        }
    }

    @Override
    public void hide(int page, UUID viewer) {
        Hologram hologram = pageAt(page);
        Player player = Bukkit.getPlayer(Objects.requireNonNull(viewer, "viewer"));
        if (player != null) {
            scheduler.entity(hologram.entity(), () -> hologram.hide(plugin, player));
        }
    }

    private Hologram pageAt(int page) {
        if (page < 0 || page >= pages.size()) {
            throw new IndexOutOfBoundsException("page " + page + " out of [0, " + pages.size() + ")");
        }
        return pages.get(page);
    }
}
