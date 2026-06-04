package com.uxplima.uxmlib.common.regex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@org.jspecify.annotations.NullUnmarked
class TimedRegexTest {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /** Counts how many tasks the matcher submitted, proving it goes through the injected executor. */
    private final AtomicInteger submitted = new AtomicInteger();

    private final ExecutorService countingExecutor = new java.util.concurrent.AbstractExecutorService() {
        @Override
        public void execute(Runnable command) {
            submitted.incrementAndGet();
            executor.execute(command);
        }

        @Override
        public void shutdown() {}

        @Override
        public List<Runnable> shutdownNow() {
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, java.util.concurrent.TimeUnit unit) {
            return true;
        }
    };

    private static void ignoreWarning(String message) {}

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    private TimedRegex timedRegex(long timeoutMs) {
        return new TimedRegex(countingExecutor, Duration.ofMillis(timeoutMs), TimedRegexTest::ignoreWarning);
    }

    @Test
    void normal_operation_runs_on_injected_executor_and_returns_result() {
        TimedRegex tr = timedRegex(200);
        String out = tr.run("ok", () -> "computed", "fallback");
        assertThat(out).isEqualTo("computed");
        assertThat(submitted.get()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void pathological_pattern_is_abandoned_within_budget_and_returns_fallback() {
        TimedRegex tr = timedRegex(50);
        Pattern evil = Pattern.compile("(a+)+$");
        // 40 a's followed by '!' makes (a+)+$ backtrack catastrophically (no match).
        String body = "x " + "a".repeat(40) + "! y";
        long start = System.nanoTime();
        String out = tr.run(
                "evil", () -> evil.matcher(TimedRegex.interruptible(body)).replaceAll("Z"), body);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        // The op was abandoned, so the fallback (unchanged body) came back.
        assertThat(out).isEqualTo(body);
        // Abandoned within a small multiple of the budget, not seconds of hang.
        assertThat(elapsedMs).isLessThan(2_000);
    }

    @Test
    void op_that_throws_returns_fallback_and_does_not_propagate() {
        TimedRegex tr = timedRegex(200);
        String out = tr.run(
                "boom",
                () -> {
                    throw new IllegalStateException("kaboom");
                },
                "fallback");
        assertThat(out).isEqualTo("fallback");
    }

    @Test
    void a_failing_op_hands_a_throttled_message_to_the_warn_sink() {
        AtomicInteger warnings = new AtomicInteger();
        TimedRegex tr = new TimedRegex(countingExecutor, Duration.ofMillis(200), message -> warnings.incrementAndGet());
        // A throwing op deterministically routes through the failure branch (no timing dependence); two
        // failures under the same id within the throttle window hand the warn sink exactly one message.
        tr.run("dup", TimedRegexTest::boom, Boolean.FALSE);
        tr.run("dup", TimedRegexTest::boom, Boolean.FALSE);
        assertThat(warnings.get()).isEqualTo(1);
    }

    private static Boolean boom() {
        throw new IllegalStateException("kaboom");
    }

    @Test
    void interruptible_wraps_a_char_sequence_view() {
        CharSequence wrapped = TimedRegex.interruptible("hello");
        assertThat(wrapped.toString()).isEqualTo("hello");
        assertThat(wrapped.length()).isEqualTo(5);
        assertThat(wrapped.charAt(0)).isEqualTo('h');
    }

    @Test
    void ctor_rejects_non_positive_timeout() {
        assertThatThrownBy(() -> new TimedRegex(countingExecutor, Duration.ZERO, TimedRegexTest::ignoreWarning))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timeout");
    }
}
