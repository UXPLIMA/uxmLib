package com.uxplima.uxmlib.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import com.uxplima.uxmlib.scheduler.Scheduler;
import com.uxplima.uxmlib.scheduler.TaskHandle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Smoke-tests the Bukkit wiring with MockBukkit: once {@link OnlineDataManager#installListener} registers the
 * listener, a player join fires a load and a disconnect fires a save plus an eviction. MockBukkit can dispatch
 * join/quit events (it cannot dispatch Brigadier), so this is the round-trip the unit test stubs out.
 */
class OnlineDataListenerTest {

    private ServerMock server;
    private Plugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("online-data");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void joinLoadsAndDisconnectSavesThenEvicts() {
        CountingStore store = new CountingStore();
        OnlineDataManager<String> manager =
                new OnlineDataManager<>(new InlineScheduler(), store, Duration.ofMinutes(5), t -> {});
        manager.installListener(plugin);

        PlayerMock player = server.addPlayer();
        UUID id = player.getUniqueId();
        assertThat(store.loads).isEqualTo(1);
        assertThat(manager.isLoaded(id)).isTrue();

        player.disconnect();
        assertThat(store.saves).isEqualTo(1);
        assertThat(manager.isLoaded(id)).isFalse();
    }

    /** A store that counts loads and saves and hands back the player's name string on load. */
    private static final class CountingStore implements DataStore<String> {
        private int loads;
        private int saves;

        @Override
        public String load(UUID id) {
            loads++;
            return id.toString();
        }

        @Override
        public void save(UUID id, String value) {
            saves++;
        }
    }

    /** A scheduler that runs async work inline; the listener test never starts the flush timer. */
    private static final class InlineScheduler implements Scheduler {
        private final Map<UUID, Boolean> unused = new ConcurrentHashMap<>();

        private final TaskHandle handle = new TaskHandle() {
            @Override
            public void cancel() {}

            @Override
            public boolean isCancelled() {
                return false;
            }
        };

        @Override
        public TaskHandle async(Runnable task) {
            task.run();
            return handle;
        }

        @Override
        public TaskHandle asyncTimer(Duration delay, Duration period, Consumer<TaskHandle> task) {
            return handle;
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
