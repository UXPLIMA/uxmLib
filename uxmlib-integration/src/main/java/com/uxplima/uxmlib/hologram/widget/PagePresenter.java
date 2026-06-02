package com.uxplima.uxmlib.hologram.widget;

import java.util.UUID;

/**
 * Where {@link PagedHologram} pushes its show/hide-by-page deltas, the same split the {@code HologramPool}
 * uses: it keeps the page state machine independent of how a page is actually shown. Production
 * ({@link DisplayPagePresenter}) maps a page index to one of N overlapping page holograms and runs the native
 * per-viewer {@code show}/{@code hide} on the entity region thread; a test records the deltas directly.
 */
public interface PagePresenter {

    /** Show the hologram for {@code page} to the player {@code viewer}. */
    void show(int page, UUID viewer);

    /** Hide the hologram for {@code page} from the player {@code viewer}. */
    void hide(int page, UUID viewer);
}
