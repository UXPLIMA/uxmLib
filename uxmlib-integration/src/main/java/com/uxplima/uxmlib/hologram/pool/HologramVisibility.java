package com.uxplima.uxmlib.hologram.pool;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * The pure, side-effect-free math the {@link HologramPool} diffs on. Two concerns live here so the pool
 * itself stays a thin scheduler shell: whether a single viewer should see a hologram, and the
 * set-difference that turns a desired viewer set into the delta of players to show and to hide.
 *
 * <p>Mirrors {@code HologramInteractions.withinReach}: the world check comes first because
 * {@link Location#distanceSquared} throws when the two locations are in different worlds.
 */
final class HologramVisibility {

    private HologramVisibility() {}

    /**
     * Whether {@code viewer} should see a hologram at {@code hologram}: both in the same (non-null) world
     * and no further than {@code radiusSquared} apart. The world check short-circuits before
     * {@link Location#distanceSquared}, which throws across worlds; a null world on either side fails it.
     */
    static boolean shouldShow(Location viewer, Location hologram, double radiusSquared) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(hologram, "hologram");
        World viewerWorld = viewer.getWorld();
        World hologramWorld = hologram.getWorld();
        if (viewerWorld == null || hologramWorld == null || !viewerWorld.equals(hologramWorld)) {
            return false;
        }
        return viewer.distanceSquared(hologram) <= radiusSquared;
    }

    /** The players that should newly see the hologram: in {@code desired} but not yet in {@code current}. */
    static Set<UUID> toShow(Set<UUID> current, Set<UUID> desired) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(desired, "desired");
        Set<UUID> show = new HashSet<>(desired);
        show.removeAll(current);
        return show;
    }

    /** The players that should stop seeing the hologram: in {@code current} but no longer in {@code desired}. */
    static Set<UUID> toHide(Set<UUID> current, Set<UUID> desired) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(desired, "desired");
        Set<UUID> hide = new HashSet<>(current);
        hide.removeAll(desired);
        return hide;
    }
}
