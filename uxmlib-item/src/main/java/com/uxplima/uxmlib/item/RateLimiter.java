package com.uxplima.uxmlib.item;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * A sliding-window rate limiter: at most {@code permits} acquisitions are allowed within any window of
 * {@code window} length. Each call to {@link #tryAcquire()} that succeeds records its timestamp; the window
 * slides as time advances, so old timestamps fall out and free up permits. This caps how fast a caller can
 * burn an external quota (the Mojang profile API behind {@link SkullResolver}, a Discord webhook, …) without
 * a fixed reset boundary that a burst could straddle.
 *
 * <p>The clock is injected as a {@link LongSupplier} of epoch millis so the window is testable with a fake
 * clock; the default reads {@link System#currentTimeMillis()}. Instances are synchronized internally and are
 * safe to share across threads.
 */
public final class RateLimiter {

    private final int permits;
    private final long windowMillis;
    private final LongSupplier clock;
    private final Deque<Long> hits = new ArrayDeque<>();

    private RateLimiter(int permits, long windowMillis, LongSupplier clock) {
        this.permits = permits;
        this.windowMillis = windowMillis;
        this.clock = clock;
    }

    /** A limiter of {@code permits} acquisitions per {@code window}, driven by the wall clock. */
    public static RateLimiter of(int permits, Duration window) {
        return of(permits, window, System::currentTimeMillis);
    }

    /**
     * A limiter of {@code permits} acquisitions per {@code window}, reading {@code clock} (epoch millis) for
     * the current time. Lets a test drive the window with a fake clock.
     */
    public static RateLimiter of(int permits, Duration window, LongSupplier clock) {
        if (permits < 1) {
            throw new IllegalArgumentException("permits must be >= 1");
        }
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(clock, "clock");
        long millis = window.toMillis();
        if (millis < 1) {
            throw new IllegalArgumentException("window must be >= 1ms");
        }
        return new RateLimiter(permits, millis, clock);
    }

    /**
     * Try to take one permit. Returns {@code true} and records the acquisition if fewer than {@code permits}
     * acquisitions fall inside the current window, otherwise {@code false} and changes nothing.
     */
    public synchronized boolean tryAcquire() {
        long now = clock.getAsLong();
        evictBefore(now - windowMillis);
        if (hits.size() >= permits) {
            return false;
        }
        hits.addLast(now);
        return true;
    }

    /** How many permits are free in the window as of now (without taking one). */
    public synchronized int available() {
        evictBefore(clock.getAsLong() - windowMillis);
        return permits - hits.size();
    }

    // Drop every recorded acquisition at or before {@code cutoff}; those have aged out of the window.
    private void evictBefore(long cutoff) {
        Long oldest;
        while ((oldest = hits.peekFirst()) != null && oldest <= cutoff) {
            hits.removeFirst();
        }
    }
}
