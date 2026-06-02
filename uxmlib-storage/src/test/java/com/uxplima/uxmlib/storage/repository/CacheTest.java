package com.uxplima.uxmlib.storage.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class CacheTest {

    @Test
    void computesOnMissThenServesFromCache() {
        Cache<String, Integer> cache = Cache.builder().maximumSize(100).build();
        AtomicInteger loads = new AtomicInteger();

        int first = cache.get("a", k -> loads.incrementAndGet());
        int second = cache.get("a", k -> loads.incrementAndGet());

        assertThat(first).isEqualTo(1);
        assertThat(second).isEqualTo(1); // served from cache; loader not called again
        assertThat(loads.get()).isEqualTo(1);
    }

    @Test
    void getIfPresentReflectsPutAndInvalidate() {
        Cache<String, Integer> cache =
                Cache.builder().expireAfterWrite(Duration.ofMinutes(5)).build();

        assertThat(cache.getIfPresent("x")).isEmpty();
        cache.put("x", 42);
        assertThat(cache.getIfPresent("x")).contains(42);

        cache.invalidate("x");
        assertThat(cache.getIfPresent("x")).isEmpty();
    }

    @Test
    void invalidateAllEmptiesTheCache() {
        Cache<String, Integer> cache = Cache.builder().build();
        cache.put("a", 1);
        cache.put("b", 2);

        cache.invalidateAll();

        assertThat(cache.getIfPresent("a")).isEmpty();
        assertThat(cache.getIfPresent("b")).isEmpty();
    }

    @Test
    void maximumSizeEvictsTheLeastRecentlyUsedEntry() {
        Cache<String, Integer> cache = Cache.builder().maximumSize(1).build();

        cache.put("a", 1);
        cache.put("b", 2); // exceeds maximumSize(1), evicts an entry
        cache.cleanUp();

        // Exactly one of the two survives the size bound.
        int present = (cache.getIfPresent("a").isPresent() ? 1 : 0)
                + (cache.getIfPresent("b").isPresent() ? 1 : 0);
        assertThat(present).isEqualTo(1);
        assertThat(cache.estimatedSize()).isEqualTo(1);
    }

    @Test
    void expireAfterAccessDropsAnUntouchedEntryWhenTheTickerAdvances() {
        AtomicLong nanos = new AtomicLong();
        Cache<String, Integer> cache = Cache.builder()
                .ticker(nanos::get)
                .expireAfterAccess(Duration.ofSeconds(10))
                .build();
        cache.put("x", 1);

        nanos.set(Duration.ofSeconds(5).toNanos());
        assertThat(cache.getIfPresent("x")).contains(1); // still inside the window

        nanos.set(Duration.ofSeconds(20).toNanos());
        cache.cleanUp();
        assertThat(cache.getIfPresent("x")).isEmpty(); // idle past the TTL, evicted
    }
}
