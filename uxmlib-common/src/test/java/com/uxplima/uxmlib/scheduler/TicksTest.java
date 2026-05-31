package com.uxplima.uxmlib.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class TicksTest {

    @Test
    void convertsSecondsToTwentyTicksEach() {
        assertThat(Ticks.fromDuration(Duration.ofSeconds(1))).isEqualTo(20L);
        assertThat(Ticks.fromDuration(Duration.ofSeconds(5))).isEqualTo(100L);
    }

    @Test
    void clampsSubTickDurationsToOne() {
        assertThat(Ticks.fromDuration(Duration.ZERO)).isEqualTo(1L);
        assertThat(Ticks.fromDuration(Duration.ofMillis(10))).isEqualTo(1L);
        assertThat(Ticks.fromDuration(Duration.ofMillis(50))).isEqualTo(1L);
    }

    @Test
    void roundsDownToWholeTicks() {
        assertThat(Ticks.fromDuration(Duration.ofMillis(99))).isEqualTo(1L);
        assertThat(Ticks.fromDuration(Duration.ofMillis(100))).isEqualTo(2L);
    }
}
