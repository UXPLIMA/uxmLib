package com.uxplima.uxmlib.storage.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.uxplima.uxmlib.scheduler.TaskHandle;
import com.uxplima.uxmlib.storage.sql.Database;
import com.uxplima.uxmlib.storage.sql.Sql;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises the row-sync lifecycle (start/stop) without Paper: the storage module's tests run no MockBukkit,
 * so the timer the service would hand to {@link com.uxplima.uxmlib.scheduler.Scheduler#asyncTimer} is supplied
 * directly through the package-private seam and fired by hand. The seam is exactly what the public
 * {@code start(Scheduler, Duration)} feeds the real scheduler.
 */
class RowSyncServiceTest {

    private Database database;
    private Sql sql;

    @BeforeEach
    void setUp() {
        database = Database.builder()
                .jdbcUrl("jdbc:sqlite:file:uxmlibrowsyncsvc?mode=memory&cache=shared")
                .maxPoolSize(1)
                .build();
        sql = new Sql(database);
        sql.execute("CREATE TABLE players ("
                + "id INTEGER PRIMARY KEY, name TEXT NOT NULL, "
                + "row_version INTEGER NOT NULL, updated_by TEXT NOT NULL)");
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    private void insert(int id, String name, long version, String updatedBy) {
        sql.update("INSERT INTO players (id, name, row_version, updated_by) VALUES (?, ?, ?, ?)", ps -> {
            ps.setInt(1, id);
            ps.setString(2, name);
            ps.setLong(3, version);
            ps.setString(4, updatedBy);
        });
    }

    private RowSyncPoller<String> poller(RowSyncListener<String> listener) {
        RowSyncConfig config = RowSyncConfig.builder("players", "id", "row_version", "updated_by", "node-A")
                .build();
        return new RowSyncPoller<>(sql, config, row -> row.getString("name"), listener);
    }

    /** A timer seam that captures the repeating body and a handle the test can inspect for cancellation. */
    private static final class CapturingTimer implements RowSyncService.RepeatingTimer {
        private @org.jspecify.annotations.Nullable Runnable body;
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private int scheduleCount;

        @Override
        public TaskHandle schedule(Duration period, Runnable tick) {
            this.body = tick;
            this.scheduleCount++;
            return new TaskHandle() {
                @Override
                public void cancel() {
                    cancelled.set(true);
                }

                @Override
                public boolean isCancelled() {
                    return cancelled.get();
                }
            };
        }

        void fire() {
            Runnable run = body;
            if (run != null) {
                run.run();
            }
        }
    }

    @Test
    void startSchedulesATimerWhoseTickRunsAPoll() {
        insert(1, "Steve", 5, "node-B");
        List<String> applied = new ArrayList<>();
        RowSyncService<String> service = new RowSyncService<>(poller(row -> applied.add(row.value())));
        CapturingTimer timer = new CapturingTimer();

        service.start(timer, Duration.ofSeconds(10));
        assertThat(timer.scheduleCount).isEqualTo(1);

        timer.fire();
        assertThat(applied).containsExactly("Steve");
    }

    @Test
    void stopCancelsTheTimerAndIsIdempotent() {
        RowSyncService<String> service = new RowSyncService<>(poller(row -> {}));
        CapturingTimer timer = new CapturingTimer();
        service.start(timer, Duration.ofSeconds(5));

        service.stop();
        assertThat(timer.cancelled).isTrue();
        assertThat(service.isRunning()).isFalse();
        // A second stop is a no-op, not a throw.
        service.stop();
    }

    @Test
    void startWhileRunningIsRejected() {
        RowSyncService<String> service = new RowSyncService<>(poller(row -> {}));
        service.start(new CapturingTimer(), Duration.ofSeconds(5));

        assertThatThrownBy(() -> service.start(new CapturingTimer(), Duration.ofSeconds(5)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void canRestartAfterStop() {
        RowSyncService<String> service = new RowSyncService<>(poller(row -> {}));
        CapturingTimer first = new CapturingTimer();
        service.start(first, Duration.ofSeconds(5));
        service.stop();

        CapturingTimer second = new CapturingTimer();
        service.start(second, Duration.ofSeconds(5));
        assertThat(service.isRunning()).isTrue();
        assertThat(second.scheduleCount).isEqualTo(1);
    }

    @Test
    void rejectsANonPositivePeriod() {
        RowSyncService<String> service = new RowSyncService<>(poller(row -> {}));
        assertThatThrownBy(() -> service.start(new CapturingTimer(), Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @SuppressWarnings("NullAway")
    void constructorAndStartRejectNulls() {
        assertThatThrownBy(() -> new RowSyncService<>(null)).isInstanceOf(NullPointerException.class);

        RowSyncService<String> service = new RowSyncService<>(poller(row -> {}));
        assertThatThrownBy(() -> service.start((RowSyncService.RepeatingTimer) null, Duration.ofSeconds(5)))
                .isInstanceOf(NullPointerException.class);
    }
}
