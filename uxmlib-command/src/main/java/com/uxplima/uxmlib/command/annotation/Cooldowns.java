package com.uxplima.uxmlib.command.annotation;

import java.util.Objects;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import org.jspecify.annotations.Nullable;

/**
 * A dependency-free, thread-safe cooldown gate keyed by an opaque string (the command path plus the
 * sender's UUID, in practice). Each key maps to the wall-clock millis at which its cooldown expires;
 * {@link #check} reports the time still to wait and re-arms when the window has elapsed. There is no
 * background task: expired entries are evicted lazily as they are checked, the model mythiclib's
 * {@code CooldownMap} uses. The clock is injectable so the arm/expire progression is unit-testable
 * without sleeping; the default is wall time.
 *
 * <p>By default windows live only in memory and are lost on restart. To make long cooldowns (a daily kit,
 * a weekly reward) survive one, back the gate with a {@link CooldownStore} via
 * {@link #Cooldowns(LongSupplier, CooldownStore)}: a cold key is read through the store, and arming a window
 * writes through it. The store is an optional seam so this module never depends on storage.
 *
 * <p>This is a plain instance with no static mutable state — construct one and share it across a set of
 * registrations through {@link ParamResolvers#cooldowns(Cooldowns)} so they all see the same windows.
 */
public final class Cooldowns {

    private final ConcurrentHashMap<String, Long> expiryByKey = new ConcurrentHashMap<>();
    private final LongSupplier clock;
    private final @Nullable CooldownStore store;

    /** A cooldown store driven by wall-clock time ({@link System#currentTimeMillis()}). */
    public Cooldowns() {
        this(System::currentTimeMillis, null);
    }

    /**
     * A cooldown store driven by {@code clock}, a supplier of "now" in epoch millis. Pass a controllable
     * supplier in tests; production code wants the no-arg constructor.
     */
    public Cooldowns(LongSupplier clock) {
        this(clock, null);
    }

    /**
     * A cooldown gate driven by {@code clock} and persisted through {@code store}, so windows armed before a
     * restart are recovered the first time their key is checked again. Pass {@code null} for {@code store} to
     * keep the windows in memory only.
     */
    public Cooldowns(LongSupplier clock, @Nullable CooldownStore store) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.store = store;
    }

    /**
     * Report and refresh the cooldown for {@code key}. If the key is still cooling down, returns the
     * positive millis left to wait and leaves the window untouched. Otherwise arms a fresh window of
     * {@code durationMillis} and returns {@code 0}. A non-positive {@code durationMillis} is a no-op that
     * always returns {@code 0} (and stores nothing).
     *
     * @param key an opaque, stable identity for the gated action; never {@code null}
     * @param durationMillis how long a freshly armed window lasts, in millis
     * @return the remaining millis to wait, or {@code 0} when the action may proceed (and was re-armed)
     */
    public long check(String key, long durationMillis) {
        Objects.requireNonNull(key, "key");
        if (durationMillis <= 0L) {
            return 0L;
        }
        long now = clock.getAsLong();
        long expiry = expiryOf(key);
        if (expiry > now) {
            return expiry - now;
        }
        arm(key, now + durationMillis);
        return 0L;
    }

    /**
     * The expiry for {@code key}: the in-memory value, or — on a cold key with a {@link CooldownStore} — the
     * persisted value pulled into memory. {@code 0} when no live window is known.
     */
    private long expiryOf(String key) {
        Long inMemory = expiryByKey.get(key);
        if (inMemory != null) {
            return inMemory;
        }
        if (store == null) {
            return 0L;
        }
        OptionalLong persisted = store.load(key);
        if (persisted.isEmpty()) {
            return 0L;
        }
        long expiry = persisted.getAsLong();
        expiryByKey.put(key, expiry);
        return expiry;
    }

    /** Arm a fresh window for {@code key} in memory and, when a store backs this gate, persist it too. */
    private void arm(String key, long expiry) {
        expiryByKey.put(key, expiry);
        if (store != null) {
            store.save(key, expiry);
        }
    }

    /** The number of live (or not-yet-evicted) entries. Exposed for tests and diagnostics. */
    public int size() {
        return expiryByKey.size();
    }
}
