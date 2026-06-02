package com.uxplima.uxmlib.hologram.widget;

import java.util.Objects;
import java.util.UUID;
import java.util.function.ToDoubleFunction;

/**
 * What a {@link SwitchSelection} predicate sees when it decides which state a viewer is in: the viewer's
 * {@link UUID} and a stat lookup. The lookup turns a stat key (a permission tier, a balance, a score) into a
 * number so a state can be selected either by an arbitrary predicate or by a simple {@code stat(key) >= n}
 * threshold, mirroring the GUI {@code RenderContext}. A {@link UUID} rather than a {@code Player} keeps the
 * context pure and unable to pin a logged-out player, so the whole selection is unit-testable.
 */
public final class ViewerContext {

    private final UUID player;
    private final ToDoubleFunction<String> stats;

    public ViewerContext(UUID player, ToDoubleFunction<String> stats) {
        this.player = Objects.requireNonNull(player, "player");
        this.stats = Objects.requireNonNull(stats, "stats");
    }

    /** The viewer this context describes. */
    public UUID player() {
        return player;
    }

    /** The numeric value of {@code key} for this viewer, as supplied by the injected lookup. */
    public double stat(String key) {
        Objects.requireNonNull(key, "key");
        return stats.applyAsDouble(key);
    }
}
