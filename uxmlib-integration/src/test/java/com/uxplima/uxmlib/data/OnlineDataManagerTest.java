package com.uxplima.uxmlib.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import com.uxplima.uxmlib.scheduler.Scheduler;
import com.uxplima.uxmlib.scheduler.TaskHandle;
import org.junit.jupiter.api.Test;

/**
 * Drives the join/quit/flush lifecycle of {@link OnlineDataManager} against fakes: a counting
 * {@link DataStore} records every load/save, a synchronous {@link Scheduler} runs async work inline and
 * captures the repeating flush timer, so one flush can be fired on demand. No live server is needed; the
 * join/quit handlers are invoked directly the same way the Bukkit listener would.
 */
class OnlineDataManagerTest {

    @Test
    void joinLoadsOnceAndCaches() {
        CountingStore store = new CountingStore();
        InlineScheduler scheduler = new InlineScheduler();
        OnlineDataManager<Profile> manager =
                new OnlineDataManager<>(scheduler, store, FLUSH, OnlineDataManagerTest::ignore);
        UUID player = UUID.randomUUID();

        manager.handleJoin(player);

        assertThat(store.loads).isEqualTo(1);
        assertThat(manager.get(player)).isNotNull();
        assertThat(manager.isLoaded(player)).isTrue();
    }

    @Test
    void getReturnsNullForAPlayerThatNeverJoined() {
        OnlineDataManager<Profile> manager = new OnlineDataManager<>(
                new InlineScheduler(), new CountingStore(), FLUSH, OnlineDataManagerTest::ignore);

        assertThat(manager.get(UUID.randomUUID())).isNull();
    }

    @Test
    void quitSavesThenEvicts() {
        CountingStore store = new CountingStore();
        OnlineDataManager<Profile> manager =
                new OnlineDataManager<>(new InlineScheduler(), store, FLUSH, OnlineDataManagerTest::ignore);
        UUID player = UUID.randomUUID();
        manager.handleJoin(player);

        manager.handleQuit(player);

        assertThat(store.saves).isEqualTo(1);
        assertThat(manager.get(player)).isNull();
        assertThat(manager.isLoaded(player)).isFalse();
    }

    @Test
    void quitForAnUnknownPlayerNeitherSavesNorThrows() {
        CountingStore store = new CountingStore();
        OnlineDataManager<Profile> manager =
                new OnlineDataManager<>(new InlineScheduler(), store, FLUSH, OnlineDataManagerTest::ignore);

        manager.handleQuit(UUID.randomUUID());

        assertThat(store.saves).isZero();
    }

    @Test
    void periodicFlushSavesEveryOnlineEntryWithoutEvicting() {
        CountingStore store = new CountingStore();
        InlineScheduler scheduler = new InlineScheduler();
        OnlineDataManager<Profile> manager =
                new OnlineDataManager<>(scheduler, store, FLUSH, OnlineDataManagerTest::ignore);
        UUID one = UUID.randomUUID();
        UUID two = UUID.randomUUID();
        manager.handleJoin(one);
        manager.handleJoin(two);
        manager.start();

        scheduler.fireFlush();

        assertThat(store.saves).isEqualTo(2);
        assertThat(manager.isLoaded(one)).isTrue();
        assertThat(manager.isLoaded(two)).isTrue();
    }

    @Test
    void startIsIdempotentAndStopCancelsTheTimer() {
        InlineScheduler scheduler = new InlineScheduler();
        OnlineDataManager<Profile> manager =
                new OnlineDataManager<>(scheduler, new CountingStore(), FLUSH, OnlineDataManagerTest::ignore);

        manager.start();
        manager.start();
        assertThat(manager.isRunning()).isTrue();
        assertThat(scheduler.timers).isEqualTo(1);

        manager.stop();
        assertThat(manager.isRunning()).isFalse();
    }

    @Test
    void aFailingLoadIsRoutedToTheErrorSinkAndLeavesThePlayerUnloaded() {
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        DataStore<Profile> boom = new DataStore<>() {
            @Override
            public Profile load(UUID id) {
                throw new IllegalStateException("load failed");
            }

            @Override
            public void save(UUID id, Profile value) {}
        };
        OnlineDataManager<Profile> manager = new OnlineDataManager<>(new InlineScheduler(), boom, FLUSH, errors::add);
        UUID player = UUID.randomUUID();

        manager.handleJoin(player);

        assertThat(errors).hasSize(1);
        assertThat(manager.isLoaded(player)).isFalse();
    }

    @Test
    void aFailingSaveInTheFlushIsRoutedToTheErrorSinkAndDoesNotStopTheRest() {
        AtomicInteger saves = new AtomicInteger();
        UUID bad = UUID.randomUUID();
        UUID good = UUID.randomUUID();
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        DataStore<Profile> store = new DataStore<>() {
            @Override
            public Profile load(UUID id) {
                return new Profile();
            }

            @Override
            public void save(UUID id, Profile value) {
                saves.incrementAndGet();
                if (id.equals(bad)) {
                    throw new IllegalStateException("save failed");
                }
            }
        };
        InlineScheduler scheduler = new InlineScheduler();
        OnlineDataManager<Profile> manager = new OnlineDataManager<>(scheduler, store, FLUSH, errors::add);
        manager.handleJoin(bad);
        manager.handleJoin(good);
        manager.start();

        scheduler.fireFlush();

        assertThat(saves.get()).isEqualTo(2);
        assertThat(errors).hasSize(1);
    }

    @Test
    void rejectsANonPositiveFlushInterval() {
        assertThatThrownBy(() -> new OnlineDataManager<>(
                        new InlineScheduler(), new CountingStore(), Duration.ZERO, OnlineDataManagerTest::ignore))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static final Duration FLUSH = Duration.ofMinutes(5);

    private static void ignore(Throwable failure) {}

    /** A trivial mutable value object the manager caches per online player. */
    private static final class Profile {}

    /** A store that counts loads and saves and hands back a fresh value on each load. */
    private static final class CountingStore implements DataStore<Profile> {
        private int loads;
        private int saves;
        private final Map<UUID, Profile> backing = new ConcurrentHashMap<>();

        @Override
        public Profile load(UUID id) {
            loads++;
            return backing.computeIfAbsent(id, k -> new Profile());
        }

        @Override
        public void save(UUID id, Profile value) {
            saves++;
            backing.put(id, value);
        }
    }

    /** A scheduler that runs async work inline and captures the repeating flush timer for on-demand firing. */
    private static final class InlineScheduler implements Scheduler {
        private @org.jspecify.annotations.Nullable Consumer<TaskHandle> flush;
        private int timers;
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
        public TaskHandle async(Runnable task) {
            task.run();
            return handle;
        }

        @Override
        public TaskHandle asyncTimer(Duration delay, Duration period, Consumer<TaskHandle> task) {
            this.flush = task;
            this.timers++;
            this.cancelled = false;
            return handle;
        }

        void fireFlush() {
            if (flush != null) {
                flush.accept(handle);
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
        public TaskHandle globalTimer(Duration delay, Duration period, Consumer<TaskHandle> task) {
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
        public TaskHandle asyncLater(Duration delay, Runnable task) {
            throw new UnsupportedOperationException();
        }
    }
}
