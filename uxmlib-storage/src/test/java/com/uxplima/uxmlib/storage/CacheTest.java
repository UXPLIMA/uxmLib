package com.uxplima.uxmlib.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

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
}
