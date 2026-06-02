package com.uxplima.uxmlib.item;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.uxplima.uxmlib.scheduler.Scheduler;

/**
 * Resolves a player name or UUID to a head {@link SkullData.ByTexture} off the main thread, caching the
 * result so a head leaderboard does not re-fetch the same profile and a shared {@link RateLimiter} so it does
 * not burn the Mojang quota. The blocking lookup lives behind a {@link ProfileCompleter} seam dispatched on
 * {@link Scheduler#async}; callers get a {@link CompletableFuture} and never block a server thread.
 *
 * <p>The cache is a bounded, access-ordered LRU keyed by the lowercased name or UUID string, and it caches
 * <em>misses</em> too (negative caching): an unknown name resolves to an empty {@link Optional} that is
 * remembered, so repeated lookups of a non-existent player do not each cost a network call. Concurrent
 * lookups of the same key share one in-flight future (single-flight) rather than each firing a fetch.
 *
 * <p>When the limiter has no permit left a lookup resolves to empty without caching, so a later call (once
 * the window slides) can still succeed.
 */
public final class SkullResolver {

    private final Scheduler scheduler;
    private final ProfileCompleter completer;
    private final RateLimiter limiter;
    private final Map<String, CompletableFuture<Optional<SkullData>>> cache;

    /** A resolver backed by the live Paper completer, 1024 cached entries and 600 lookups/minute. */
    public static SkullResolver create(Scheduler scheduler) {
        return new SkullResolver(
                scheduler, new PaperProfileCompleter(), RateLimiter.of(600, Duration.ofMinutes(1)), 1024);
    }

    /**
     * @param scheduler the library scheduler; the completer runs on its async pool
     * @param completer the (blocking) name/uuid → texture lookup
     * @param limiter caps how fast lookups hit the completer
     * @param maxEntries the LRU cache bound (least-recently-used entries are evicted past it)
     */
    public SkullResolver(Scheduler scheduler, ProfileCompleter completer, RateLimiter limiter, int maxEntries) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.completer = Objects.requireNonNull(completer, "completer");
        this.limiter = Objects.requireNonNull(limiter, "limiter");
        if (maxEntries < 1) {
            throw new IllegalArgumentException("maxEntries must be >= 1");
        }
        this.cache = boundedLru(maxEntries);
    }

    /** Resolve the player named {@code name} to a head texture (empty if unknown or skinless). */
    public CompletableFuture<Optional<SkullData>> resolveName(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return resolve(name.toLowerCase(java.util.Locale.ROOT));
    }

    /** Resolve the account with {@code uuid} to a head texture (empty if unknown or skinless). */
    public CompletableFuture<Optional<SkullData>> resolveUuid(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return resolve(uuid.toString());
    }

    /**
     * Resolve {@code data} to a head texture: {@link SkullData.ByTexture} is already resolved and returned
     * as-is; a {@link SkullData.ByName} or {@link SkullData.ByUuid} is looked up. Lets a config string parsed
     * by {@link SkullData#parse(String)} flow through one call.
     */
    public CompletableFuture<Optional<SkullData>> resolve(SkullData data) {
        Objects.requireNonNull(data, "data");
        return switch (data) {
            case SkullData.ByTexture texture -> CompletableFuture.completedFuture(Optional.of(texture));
            case SkullData.ByName name -> resolveName(name.name());
            case SkullData.ByUuid id -> resolveUuid(id.uuid());
        };
    }

    private CompletableFuture<Optional<SkullData>> resolve(String key) {
        synchronized (cache) {
            CompletableFuture<Optional<SkullData>> cached = cache.get(key);
            if (cached != null) {
                return cached;
            }
            CompletableFuture<Optional<SkullData>> pending = new CompletableFuture<>();
            cache.put(key, pending);
            scheduler.async(() -> fetchInto(key, pending));
            return pending;
        }
    }

    // Runs on the async pool. On a rate-limit miss the entry is dropped so a later window can retry; any
    // other outcome (hit or negative) stays cached. The completer's own exceptions resolve to a dropped miss.
    private void fetchInto(String key, CompletableFuture<Optional<SkullData>> pending) {
        if (!limiter.tryAcquire()) {
            forget(key);
            pending.complete(Optional.empty());
            return;
        }
        try {
            Optional<SkullData> result = completer.complete(key).map(texture -> (SkullData) texture);
            pending.complete(result);
        } catch (RuntimeException failure) {
            forget(key);
            pending.completeExceptionally(failure);
        }
    }

    private void forget(String key) {
        synchronized (cache) {
            cache.remove(key);
        }
    }

    private static Map<String, CompletableFuture<Optional<SkullData>>> boundedLru(int maxEntries) {
        return new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CompletableFuture<Optional<SkullData>>> eldest) {
                return size() > maxEntries;
            }
        };
    }
}
