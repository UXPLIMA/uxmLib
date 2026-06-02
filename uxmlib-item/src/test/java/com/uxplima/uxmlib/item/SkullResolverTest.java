package com.uxplima.uxmlib.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class SkullResolverTest {

    private static final SkullData.ByTexture NOTCH = new SkullData.ByTexture("notch-texture-base64-value");

    // A completer that records every key it is asked for, so tests can assert the cache spared it a call.
    private static final class CountingCompleter implements ProfileCompleter {
        private final Map<String, SkullData.ByTexture> known;
        private final AtomicInteger calls = new AtomicInteger();

        CountingCompleter(Map<String, SkullData.ByTexture> known) {
            this.known = known;
        }

        @Override
        public Optional<SkullData.ByTexture> complete(String key) {
            calls.incrementAndGet();
            return Optional.ofNullable(known.get(key));
        }
    }

    private SkullResolver resolver(CountingCompleter completer) {
        return new SkullResolver(new ItemInlineScheduler(), completer, RateLimiter.of(100, Duration.ofMinutes(1)), 64);
    }

    @Test
    void resolvesAKnownNameToItsTexture() {
        CountingCompleter completer = new CountingCompleter(Map.of("notch", NOTCH));
        SkullResolver resolver = resolver(completer);

        Optional<SkullData> result = resolver.resolveName("Notch").join();

        assertThat(result).contains(NOTCH);
    }

    @Test
    void cacheHitSparesASecondLookup() {
        CountingCompleter completer = new CountingCompleter(Map.of("notch", NOTCH));
        SkullResolver resolver = resolver(completer);

        resolver.resolveName("Notch").join();
        resolver.resolveName("Notch").join();
        resolver.resolveName("NOTCH").join(); // case-folded to the same key

        assertThat(completer.calls).hasValue(1);
    }

    @Test
    void negativeResultIsCachedSoMissesDoNotRefetch() {
        CountingCompleter completer = new CountingCompleter(Map.of());
        SkullResolver resolver = resolver(completer);

        assertThat(resolver.resolveName("ghost").join()).isEmpty();
        assertThat(resolver.resolveName("ghost").join()).isEmpty();

        assertThat(completer.calls).hasValue(1);
    }

    @Test
    void resolvesAUuidKey() {
        UUID id = UUID.randomUUID();
        CountingCompleter completer = new CountingCompleter(Map.of(id.toString(), NOTCH));
        SkullResolver resolver = resolver(completer);

        assertThat(resolver.resolveUuid(id).join()).contains(NOTCH);
    }

    @Test
    void textureSkullDataPassesThroughWithoutALookup() {
        CountingCompleter completer = new CountingCompleter(Map.of());
        SkullResolver resolver = resolver(completer);

        Optional<SkullData> result = resolver.resolve(NOTCH).join();

        assertThat(result).contains(NOTCH);
        assertThat(completer.calls).hasValue(0);
    }

    @Test
    void rateLimitedLookupResolvesEmptyAndIsNotCached() {
        AtomicLong clock = new AtomicLong(0);
        CountingCompleter completer = new CountingCompleter(new HashMap<>(Map.of("notch", NOTCH)));
        RateLimiter limiter = RateLimiter.of(1, Duration.ofSeconds(1), clock::get);
        SkullResolver resolver = new SkullResolver(new ItemInlineScheduler(), completer, limiter, 64);

        // First lookup spends the only permit on an unknown key.
        assertThat(resolver.resolveName("ghost").join()).isEmpty();
        // Second (different) key has no permit: empty, and crucially NOT cached, so it can retry later.
        assertThat(resolver.resolveName("notch").join()).isEmpty();

        // Window slides; the retry now finds a permit and resolves the real texture.
        clock.set(1_001);
        assertThat(resolver.resolveName("notch").join()).contains(NOTCH);
    }

    @Test
    void completerFailurePropagatesAndIsNotCached() {
        AtomicInteger calls = new AtomicInteger();
        ProfileCompleter flaky = key -> {
            if (calls.getAndIncrement() == 0) {
                throw new IllegalStateException("boom");
            }
            return Optional.of(NOTCH);
        };
        SkullResolver resolver =
                new SkullResolver(new ItemInlineScheduler(), flaky, RateLimiter.of(100, Duration.ofMinutes(1)), 64);

        assertThat(resolver.resolveName("notch")).isCompletedExceptionally();
        // The failed lookup was not cached, so a retry runs the completer again and now succeeds.
        assertThat(resolver.resolveName("notch").join()).contains(NOTCH);
    }

    @Test
    void rejectsBlankName() {
        SkullResolver resolver = resolver(new CountingCompleter(Map.of()));
        assertThatThrownBy(() -> resolver.resolveName(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}
