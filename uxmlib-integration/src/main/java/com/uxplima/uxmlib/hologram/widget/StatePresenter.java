package com.uxplima.uxmlib.hologram.widget;

import java.util.UUID;

/**
 * Where {@link SwitchableHologram} pushes its show/hide-by-state deltas, mirroring {@link PagePresenter}.
 * Production maps a state value to one of the overlapping state holograms and runs the native per-viewer
 * {@code show}/{@code hide} on the entity region thread; a test records the deltas directly.
 *
 * @param <T> the state value type the widget switches between
 */
public interface StatePresenter<T> {

    /** Show the hologram for {@code state} to the player {@code viewer}. */
    void show(T state, UUID viewer);

    /** Hide the hologram for {@code state} from the player {@code viewer}. */
    void hide(T state, UUID viewer);
}
