package com.uxplima.uxmlib.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class WarnOnceTest {

    @Test
    void logsTheFirstTimeAndSuppressesAfterwards() {
        AtomicInteger calls = new AtomicInteger();
        WarnOnce warn = new WarnOnce(message -> calls.incrementAndGet());

        assertThat(warn.warn("deprecated.x", "x is deprecated")).isTrue();
        assertThat(warn.warn("deprecated.x", "x is deprecated")).isFalse();
        assertThat(warn.warn("deprecated.x", "x is deprecated")).isFalse();

        assertThat(calls).hasValue(1);
    }

    @Test
    void forwardsTheMessageToTheSinkOnce() {
        List<String> sink = new ArrayList<>();
        WarnOnce warn = new WarnOnce(sink::add);

        warn.warn("k", "the warning");
        warn.warn("k", "the warning");

        assertThat(sink).containsExactly("the warning");
    }

    @Test
    void distinctKeysEachWarnOnce() {
        AtomicInteger calls = new AtomicInteger();
        WarnOnce warn = new WarnOnce(message -> calls.incrementAndGet());

        warn.warn("a", "a");
        warn.warn("b", "b");

        assertThat(calls).hasValue(2);
    }

    @Test
    void tracksWhetherAKeyWasWarned() {
        WarnOnce warn = new WarnOnce(message -> {});

        assertThat(warn.hasWarned("k")).isFalse();
        warn.warn("k", "msg");
        assertThat(warn.hasWarned("k")).isTrue();
    }

    @Test
    void resetLetsAKeyWarnAgain() {
        AtomicInteger calls = new AtomicInteger();
        WarnOnce warn = new WarnOnce(message -> calls.incrementAndGet());

        warn.warn("k", "msg");
        warn.reset();
        warn.warn("k", "msg");

        assertThat(calls).hasValue(2);
    }
}
