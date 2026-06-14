package com.uxplima.uxmlib.nametag;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;

import com.uxplima.uxmlib.nametag.internal.TrackedNametag;
import com.uxplima.uxmlib.scheduler.Scheduler;
import com.uxplima.uxmlib.scheduler.TaskHandle;

/**
 * The testable core of the packet nametag layer: it shows a single floating line above a target, rendered
 * per viewer through the {@link NametagPackets} port, and keeps it fresh on a region-thread refresh task. No
 * NMS — the packets are opaque objects from the port — so this whole class runs under a fake port and a fake
 * {@link Scheduler}. Multi-line text, animation, and line-of-sight fading are layered on in a later task.
 *
 * <h2>Threading</h2>
 *
 * The refresh runs through {@link Scheduler#entityTimer}, so it executes on the target's region thread — the
 * one thread where reading the target's position and resolving online viewers is safe. Nothing here touches
 * the Bukkit API from any other thread.
 */
public final class NametagRenderer {

    /** Refresh cadence when a caller does not specify one: every half-second. */
    public static final Duration DEFAULT_REFRESH_PERIOD = Duration.ofMillis(500);

    private final NametagPackets packets;
    private final Scheduler scheduler;

    public NametagRenderer(NametagPackets packets, Scheduler scheduler) {
        this.packets = Objects.requireNonNull(packets, "packets");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    /** Show {@code target}'s nametag to {@code viewers} with the default refresh period. */
    public NametagHandle show(Player target, Appearance appearance, Set<UUID> viewers, PerViewerText text) {
        return show(target, appearance, viewers, text, DEFAULT_REFRESH_PERIOD);
    }

    /**
     * Allocate one display entity, spawn it for every resolvable viewer, and start a region-thread refresh
     * loop that reconciles viewers and per-viewer text every {@code period}.
     *
     * @return a handle to update or remove the nametag
     */
    public NametagHandle show(
            Player target, Appearance appearance, Set<UUID> viewers, PerViewerText text, Duration period) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(appearance, "appearance");
        Objects.requireNonNull(viewers, "viewers");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(period, "period");
        int entityId = packets.allocateEntityId();
        TrackedNametag tracked = new TrackedNametag(packets, target, entityId, viewers, text, appearance);
        tracked.spawnAll();
        TaskHandle refresh = scheduler.entityTimer(target, period, period, taskHandle -> tracked.update());
        tracked.bindRefreshTask(refresh);
        return tracked;
    }
}
