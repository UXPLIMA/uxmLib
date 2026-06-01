package com.uxplima.uxmlib.hologram.pool;

import java.util.UUID;

import com.uxplima.uxmlib.hologram.Hologram;

/**
 * Where the {@link HologramPool} pushes the show/hide deltas it computes. Splitting this out keeps the
 * pool's diff lifecycle independent of how a viewer is actually shown or hidden: production dispatches the
 * native {@code show}/{@code hide} onto the hologram entity's region thread, while a test can record the
 * deltas directly.
 */
interface ViewerSink {

    /** Make {@code hologram} visible to the player with {@code viewer}. */
    void show(Hologram hologram, UUID viewer);

    /** Hide {@code hologram} from the player with {@code viewer}. */
    void hide(Hologram hologram, UUID viewer);
}
