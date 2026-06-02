package com.uxplima.uxmlib.scheduler;

import java.time.Duration;
import java.util.Objects;

/**
 * Conversions between {@link Duration} and Minecraft server ticks. A tick is 50&nbsp;ms (20&nbsp;ticks
 * per second). The library's scheduler takes {@link Duration} everywhere so callers never hand-roll
 * tick arithmetic.
 */
public final class Ticks {

    /** Milliseconds in one server tick. */
    public static final long MILLIS_PER_TICK = 50L;

    /** One server tick as a {@link Duration}; the natural period for a per-tick timer. */
    public static final Duration ONE_TICK = Duration.ofMillis(MILLIS_PER_TICK);

    private Ticks() {}

    /**
     * The number of ticks spanning {@code duration}, clamped to at least one. Paper's region and entity
     * schedulers reject a delay or period below one tick, so this never returns zero.
     */
    public static long fromDuration(Duration duration) {
        Objects.requireNonNull(duration, "duration");
        return Math.max(1L, duration.toMillis() / MILLIS_PER_TICK);
    }
}
