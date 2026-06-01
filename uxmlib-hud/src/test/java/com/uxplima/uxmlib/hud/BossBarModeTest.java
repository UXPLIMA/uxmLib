package com.uxplima.uxmlib.hud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * The progress ramp is pure arithmetic over elapsed/total, so it is unit-tested directly without any
 * Bukkit plumbing. FILLING climbs 0..1, COUNTDOWN falls 1..0, and both clamp once the window has passed
 * so a late tick never reads a negative or over-full progress.
 */
class BossBarModeTest {

    @Test
    void fillingClimbsFromZeroToOne() {
        assertThat(BossBarMode.FILLING.progressAt(0L, 1000L)).isEqualTo(0.0f);
        assertThat(BossBarMode.FILLING.progressAt(500L, 1000L)).isEqualTo(0.5f);
        assertThat(BossBarMode.FILLING.progressAt(1000L, 1000L)).isEqualTo(1.0f);
    }

    @Test
    void countdownFallsFromOneToZero() {
        assertThat(BossBarMode.COUNTDOWN.progressAt(0L, 1000L)).isEqualTo(1.0f);
        assertThat(BossBarMode.COUNTDOWN.progressAt(250L, 1000L)).isEqualTo(0.75f);
        assertThat(BossBarMode.COUNTDOWN.progressAt(1000L, 1000L)).isEqualTo(0.0f);
    }

    @Test
    void rampClampsPastTheWindow() {
        assertThat(BossBarMode.FILLING.progressAt(5000L, 1000L)).isEqualTo(1.0f);
        assertThat(BossBarMode.COUNTDOWN.progressAt(5000L, 1000L)).isEqualTo(0.0f);
    }

    @Test
    void rampClampsBeforeTheWindow() {
        assertThat(BossBarMode.FILLING.progressAt(-50L, 1000L)).isEqualTo(0.0f);
        assertThat(BossBarMode.COUNTDOWN.progressAt(-50L, 1000L)).isEqualTo(1.0f);
    }

    @Test
    void timedModesAreFinished() {
        assertThat(BossBarMode.FILLING.timed()).isTrue();
        assertThat(BossBarMode.COUNTDOWN.timed()).isTrue();
        assertThat(BossBarMode.PERMANENT.timed()).isFalse();
        assertThat(BossBarMode.DYNAMIC.timed()).isFalse();
    }

    @Test
    void onlyCountdownAutoHidesAtTheEnd() {
        assertThat(BossBarMode.COUNTDOWN.finishedAt(1000L, 1000L)).isTrue();
        assertThat(BossBarMode.COUNTDOWN.finishedAt(999L, 1000L)).isFalse();
        assertThat(BossBarMode.FILLING.finishedAt(2000L, 1000L)).isFalse();
        assertThat(BossBarMode.PERMANENT.finishedAt(2000L, 1000L)).isFalse();
    }

    @Test
    void untimedModesRejectTheRamp() {
        assertThatThrownBy(() -> BossBarMode.PERMANENT.progressAt(0L, 1000L)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> BossBarMode.DYNAMIC.progressAt(0L, 1000L)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void nonPositiveTotalIsRejected() {
        assertThatThrownBy(() -> BossBarMode.FILLING.progressAt(0L, 0L)).isInstanceOf(IllegalArgumentException.class);
    }
}
