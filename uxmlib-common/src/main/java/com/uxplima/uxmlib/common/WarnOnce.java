package com.uxplima.uxmlib.common;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * De-duplicates noisy warnings. A given key is reported through the sink the first time it is seen and
 * suppressed on every later call, so a deprecated config value or a missing soft-dependency is logged once
 * rather than on every reload or every tick.
 *
 * <p>State is per instance (no static mutable state): a plugin keeps one {@code WarnOnce} and the seen keys
 * live and die with it. The seen set is concurrent, so warnings raised from async tasks de-duplicate safely.
 * The sink is injected, which lets tests assert against a counter and lets callers route to any logger.
 */
public final class WarnOnce {

    private final Consumer<String> sink;
    private final Set<String> seen = ConcurrentHashMap.newKeySet();

    /** Build a warn-once gate that forwards first-seen messages to {@code sink}. */
    public WarnOnce(Consumer<String> sink) {
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    /**
     * Report {@code message} under {@code key} the first time {@code key} is seen; suppress it afterwards.
     *
     * @return {@code true} if the message was forwarded to the sink, {@code false} if it was suppressed.
     */
    public boolean warn(String key, String message) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(message, "message");
        if (!seen.add(key)) {
            return false;
        }
        sink.accept(message);
        return true;
    }

    /** Whether {@code key} has already been warned about. */
    public boolean hasWarned(String key) {
        Objects.requireNonNull(key, "key");
        return seen.contains(key);
    }

    /** Forget every seen key, so the next {@link #warn} for each will fire again. Mainly for a reload. */
    public void reset() {
        seen.clear();
    }
}
