package com.uxplima.uxmlib.command.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

/**
 * Covers the optional {@link CooldownStore} seam behind {@link Cooldowns}: arming a window writes through to
 * the store, and a fresh gate (a "restart") recovers a still-live window by reading through it. A fake
 * in-map store plus a controllable clock make the persistence deterministic without real storage.
 */
class CooldownStoreTest {

    /** A trivial store standing in for a storage-backed one: a map of key to expiry millis. */
    private static final class FakeStore implements CooldownStore {
        private final Map<String, Long> saved = new HashMap<>();

        @Override
        public OptionalLong load(String key) {
            Long expiry = saved.get(key);
            return expiry == null ? OptionalLong.empty() : OptionalLong.of(expiry);
        }

        @Override
        public void save(String key, long expiryMillis) {
            saved.put(key, expiryMillis);
        }
    }

    @Test
    void armingAWindowWritesThroughToTheStore() {
        AtomicLong now = new AtomicLong(1_000L);
        FakeStore store = new FakeStore();
        Cooldowns cooldowns = new Cooldowns(now::get, store);

        cooldowns.check("k", 5_000L);

        assertThat(store.load("k")).hasValue(6_000L);
    }

    @Test
    void aFreshGateRecoversAStillLiveWindowFromTheStore() {
        AtomicLong now = new AtomicLong(1_000L);
        FakeStore store = new FakeStore();
        new Cooldowns(now::get, store).check("k", 5_000L); // armed until 6_000, persisted

        // A "restart": a brand-new in-memory gate over the same store, partway through the window.
        now.set(2_500L);
        Cooldowns afterRestart = new Cooldowns(now::get, store);

        assertThat(afterRestart.check("k", 5_000L)).isEqualTo(3_500L);
    }

    @Test
    void aFreshGateIgnoresAnAlreadyExpiredPersistedWindow() {
        AtomicLong now = new AtomicLong(1_000L);
        FakeStore store = new FakeStore();
        new Cooldowns(now::get, store).check("k", 5_000L); // armed until 6_000

        now.set(6_000L); // exactly at expiry: the window has elapsed
        Cooldowns afterRestart = new Cooldowns(now::get, store);

        // The recovered window is expired, so the gate re-arms instead of vetoing.
        assertThat(afterRestart.check("k", 5_000L)).isZero();
        assertThat(store.load("k")).hasValue(11_000L);
    }

    @Test
    void aColdKeyWithNoPersistedWindowSimplyArms() {
        AtomicLong now = new AtomicLong(0L);
        Cooldowns cooldowns = new Cooldowns(now::get, new FakeStore());

        assertThat(cooldowns.check("never-seen", 1_000L)).isZero();
    }
}
