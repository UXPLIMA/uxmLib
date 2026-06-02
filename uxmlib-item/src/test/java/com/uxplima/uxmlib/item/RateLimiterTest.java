package com.uxplima.uxmlib.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class RateLimiterTest {

    @Test
    void allowsExactlyThePermitsInTheWindowThenDenies() {
        AtomicLong clock = new AtomicLong(0);
        RateLimiter limiter = RateLimiter.of(3, Duration.ofSeconds(1), clock::get);

        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isFalse();
    }

    @Test
    void refillsAsTheWindowSlidesPastOldHits() {
        AtomicLong clock = new AtomicLong(0);
        RateLimiter limiter = RateLimiter.of(2, Duration.ofSeconds(1), clock::get);

        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isFalse();

        // Advance past the window of the first two hits; both age out and free their permits.
        clock.set(1_001);
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isFalse();
    }

    @Test
    void slidesPartially() {
        AtomicLong clock = new AtomicLong(0);
        RateLimiter limiter = RateLimiter.of(2, Duration.ofSeconds(1), clock::get);

        assertThat(limiter.tryAcquire()).isTrue(); // at t=0
        clock.set(500);
        assertThat(limiter.tryAcquire()).isTrue(); // at t=500
        assertThat(limiter.tryAcquire()).isFalse(); // full

        // At t=1001 only the t=0 hit has aged out; the t=500 hit is still in window, so one permit frees.
        clock.set(1_001);
        assertThat(limiter.available()).isEqualTo(1);
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isFalse();
    }

    @Test
    void availableReflectsFreePermits() {
        AtomicLong clock = new AtomicLong(0);
        RateLimiter limiter = RateLimiter.of(4, Duration.ofSeconds(1), clock::get);

        assertThat(limiter.available()).isEqualTo(4);
        limiter.tryAcquire();
        limiter.tryAcquire();
        assertThat(limiter.available()).isEqualTo(2);
    }

    @Test
    void rejectsNonPositiveConfiguration() {
        assertThatThrownBy(() -> RateLimiter.of(0, Duration.ofSeconds(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RateLimiter.of(1, Duration.ZERO)).isInstanceOf(IllegalArgumentException.class);
    }
}
