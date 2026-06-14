package com.uxplima.uxmlib.nametag;

import java.util.Set;
import java.util.UUID;

/**
 * A live handle to one target's nametag. Returned by {@link NametagRenderer#show}. The renderer's own refresh
 * task drives {@link #update()} on a period; a caller can also push a new viewer set, text, or appearance with
 * {@link #update(Set, PerViewerText, Appearance)} and call {@link #remove()} to tear the nametag down.
 */
public interface NametagHandle {

    /** Recompute viewers and per-viewer text against the currently held values and reconcile the clients. */
    void update();

    /**
     * Replace the held viewer set, text provider, and appearance, then reconcile: new viewers are spawned,
     * departed viewers are removed, and remaining viewers get refreshed text.
     */
    void update(Set<UUID> viewers, PerViewerText text, Appearance appearance);

    /** Despawn the nametag for every current viewer and stop its refresh task. Safe to call more than once. */
    void remove();
}
