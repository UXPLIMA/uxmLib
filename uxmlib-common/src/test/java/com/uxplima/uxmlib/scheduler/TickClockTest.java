package com.uxplima.uxmlib.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import org.junit.jupiter.api.Test;

class TickClockTest {

    /** A Scheduler that captures the global timer so the test can fire ticks deterministically. */
    private static final class FakeScheduler implements Scheduler {
        private @org.jspecify.annotations.Nullable Consumer<TaskHandle> timer;
        private boolean cancelled;

        private final TaskHandle handle = new TaskHandle() {
            @Override
            public void cancel() {
                cancelled = true;
            }

            @Override
            public boolean isCancelled() {
                return cancelled;
            }
        };

        @Override
        public TaskHandle globalTimer(Duration delay, Duration period, Consumer<TaskHandle> task) {
            this.timer = task;
            return handle;
        }

        void tick() {
            if (timer != null && !cancelled) {
                timer.accept(handle);
            }
        }

        // Unused members.
        @Override
        public TaskHandle global(Runnable task) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TaskHandle globalLater(Duration delay, Runnable task) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TaskHandle region(Location location, Runnable task) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TaskHandle regionLater(Location location, Duration delay, Runnable task) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TaskHandle regionTimer(Location location, Duration delay, Duration period, Consumer<TaskHandle> task) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TaskHandle entity(Entity entity, Runnable task) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TaskHandle entityLater(Entity entity, Duration delay, Runnable task) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TaskHandle entityTimer(Entity entity, Duration delay, Duration period, Consumer<TaskHandle> task) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TaskHandle async(Runnable task) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TaskHandle asyncLater(Duration delay, Runnable task) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TaskHandle asyncTimer(Duration delay, Duration period, Consumer<TaskHandle> task) {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    void startsAtZero() {
        TickClock clock = new TickClock(new FakeScheduler());
        assertThat(clock.ticks()).isZero();
    }

    @Test
    void advancesOncePerTick() {
        FakeScheduler scheduler = new FakeScheduler();
        TickClock clock = new TickClock(scheduler);
        clock.start();

        scheduler.tick();
        scheduler.tick();
        scheduler.tick();

        assertThat(clock.ticks()).isEqualTo(3L);
    }

    @Test
    void startIsIdempotentSoTheCountAdvancesOnce() {
        FakeScheduler scheduler = new FakeScheduler();
        TickClock clock = new TickClock(scheduler);
        clock.start();
        clock.start();

        scheduler.tick();

        assertThat(clock.ticks()).isEqualTo(1L);
    }

    @Test
    void stopFreezesTheCountAndCancelsTheTimer() {
        FakeScheduler scheduler = new FakeScheduler();
        TickClock clock = new TickClock(scheduler);
        clock.start();
        scheduler.tick();
        clock.stop();

        scheduler.tick();

        assertThat(clock.ticks()).isEqualTo(1L);
        assertThat(scheduler.handle.isCancelled()).isTrue();
    }
}
