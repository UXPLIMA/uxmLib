package com.uxplima.uxmlib.nametag.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;

import com.uxplima.uxmlib.nametag.Appearance;
import com.uxplima.uxmlib.nametag.NametagHandle;
import com.uxplima.uxmlib.nametag.NametagPackets;
import com.uxplima.uxmlib.nametag.PerViewerText;
import com.uxplima.uxmlib.scheduler.TaskHandle;
import org.jspecify.annotations.Nullable;

/**
 * One target's live packet nametag: the single display entity id, the target it rides, and the viewer set /
 * text / appearance it was last reconciled against. Owns the spawn/metadata/mount/remove packet building and
 * the per-viewer diff. Created and driven by {@code NametagRenderer}; runs on the target's region thread.
 *
 * <p>Field-ownership: every field is touched only from the target's region thread (the {@code show} call and
 * the entity-timer refresh both run there), so no synchronisation is needed.
 */
public final class TrackedNametag implements NametagHandle {

    private final NametagPackets packets;
    private final Player target;
    private final int entityId;

    private @Nullable TaskHandle refreshTask;
    private Set<UUID> viewers;
    private PerViewerText text;
    private Appearance appearance;
    private boolean removed;

    public TrackedNametag(
            NametagPackets packets,
            Player target,
            int entityId,
            Set<UUID> viewers,
            PerViewerText text,
            Appearance appearance) {
        this.packets = Objects.requireNonNull(packets, "packets");
        this.target = Objects.requireNonNull(target, "target");
        this.entityId = entityId;
        this.viewers = new HashSet<>(Objects.requireNonNull(viewers, "viewers"));
        this.text = Objects.requireNonNull(text, "text");
        this.appearance = Objects.requireNonNull(appearance, "appearance");
    }

    /** Send the full spawn bundle to each resolvable viewer in the initial set. */
    public void spawnAll() {
        for (UUID viewer : viewers) {
            spawnFor(viewer);
        }
    }

    /** The display entity id, stable for this nametag's whole life. Test/observability seam. */
    public int entityId() {
        return entityId;
    }

    /** Attach the refresh task so {@link #remove()} can cancel it. */
    public void bindRefreshTask(TaskHandle handle) {
        this.refreshTask = Objects.requireNonNull(handle, "handle");
    }

    @Override
    public void update() {
        update(viewers, text, appearance);
    }

    @Override
    public void update(Set<UUID> nextViewers, PerViewerText nextText, Appearance nextAppearance) {
        Objects.requireNonNull(nextViewers, "nextViewers");
        Objects.requireNonNull(nextText, "nextText");
        Objects.requireNonNull(nextAppearance, "nextAppearance");
        if (removed) {
            return;
        }
        this.text = nextText;
        this.appearance = nextAppearance;
        Set<UUID> next = new HashSet<>(nextViewers);
        for (UUID viewer : next) {
            reconcileViewer(viewer);
        }
        removeDepartedViewers(next);
        this.viewers = next;
    }

    /** A still-present viewer gets refreshed metadata; a newly-added one gets the full spawn bundle. */
    private void reconcileViewer(UUID viewer) {
        if (viewers.contains(viewer)) {
            sendTo(viewer, metadataPacket(viewer));
        } else {
            spawnFor(viewer);
        }
    }

    /** Send a remove packet to every viewer who was present last cycle but is not in {@code next}. */
    private void removeDepartedViewers(Set<UUID> next) {
        for (UUID viewer : viewers) {
            if (!next.contains(viewer)) {
                sendTo(viewer, packets.removePacket(new int[] {entityId}));
            }
        }
    }

    @Override
    public void remove() {
        if (removed) {
            return;
        }
        removed = true;
        Object removePacket = packets.removePacket(new int[] {entityId});
        for (UUID viewer : viewers) {
            sendTo(viewer, removePacket);
        }
        viewers = Set.of();
        if (refreshTask != null) {
            refreshTask.cancel();
        }
    }

    /** Build and send the spawn+metadata+mount bundle to one viewer. */
    private void spawnFor(UUID viewer) {
        List<Object> frame = new ArrayList<>(3);
        frame.add(packets.spawnPacket(entityId, target.getX(), target.getY(), target.getZ()));
        frame.add(metadataPacket(viewer));
        frame.add(packets.mountPacket(target.getEntityId(), new int[] {entityId}));
        sendTo(viewer, packets.bundle(frame));
    }

    /** The metadata packet carrying this viewer's first line at full opacity. */
    private Object metadataPacket(UUID viewer) {
        Component line = firstLineFor(viewer);
        return packets.metadataPacket(entityId, line, appearance, Appearance.FULL_OPACITY, appearance.translation());
    }

    private Component firstLineFor(UUID viewer) {
        List<Component> lines = text.linesFor(viewer);
        if (lines.isEmpty()) {
            throw new IllegalStateException("PerViewerText returned no lines for viewer " + viewer);
        }
        return lines.get(0);
    }

    /** Resolve the viewer to an online player and write the packet; a missing player is a silent skip. */
    private void sendTo(UUID viewer, Object packet) {
        @Nullable Player player = target.getServer().getPlayer(viewer);
        if (player != null) {
            packets.send(player, packet);
        }
    }
}
