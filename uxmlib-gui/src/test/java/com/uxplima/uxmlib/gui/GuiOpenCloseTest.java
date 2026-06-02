package com.uxplima.uxmlib.gui;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.inventory.InventoryCloseEvent;

import com.uxplima.uxmlib.scheduler.Scheduler;
import com.uxplima.uxmlib.scheduler.TaskHandle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Covers the open/close guards: a menu defers opening over a sleeping player (#17) and a preventClose menu
 * reopens itself on a client-driven close while a deliberate close still works (#15).
 */
class GuiOpenCloseTest {

    /** A Scheduler that records the entityLater tasks so a test can fire them by hand. */
    private static final class CapturingScheduler implements Scheduler {
        private final List<Runnable> deferred = new ArrayList<>();

        private final TaskHandle handle = new TaskHandle() {
            @Override
            public void cancel() {}

            @Override
            public boolean isCancelled() {
                return false;
            }
        };

        void runDeferred() {
            List<Runnable> pending = new ArrayList<>(deferred);
            deferred.clear();
            pending.forEach(Runnable::run);
        }

        @Override
        public TaskHandle entityLater(Entity entity, Duration delay, Runnable task) {
            deferred.add(task);
            return handle;
        }

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
            return handle; // the animation registry starts its shared timer here; nothing to fire for these tests
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
            task.run();
            return handle;
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

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void openIsDeferredWhileThePlayerIsSleeping() {
        CapturingScheduler scheduler = new CapturingScheduler();
        Guis.install(MockBukkit.createMockPlugin(), scheduler);
        try {
            SimpleGui gui = Guis.gui().rows(1).build();
            PlayerMock player = server.addPlayer();
            player.setSleeping(true);

            gui.open(player);

            // While asleep the menu stays closed: no top inventory is the menu's.
            assertThat(gui.getInventory().getViewers()).doesNotContain(player);

            player.setSleeping(false);
            scheduler.runDeferred(); // the deferred retry fires once they are up

            assertThat(gui.getInventory().getViewers()).contains(player);
        } finally {
            Guis.uninstall();
        }
    }

    @Test
    void preventCloseReopensOnAClientClose() {
        CapturingScheduler scheduler = new CapturingScheduler();
        Guis.install(MockBukkit.createMockPlugin(), scheduler);
        try {
            SimpleGui gui = Guis.gui().rows(1).build();
            gui.preventClose(true);
            assertThat(gui.preventsClose()).isTrue();
            PlayerMock player = server.addPlayer();
            gui.open(player);

            // Simulate the player pressing Escape: the listener routes the close to handleClose.
            gui.handleClose(new InventoryCloseEvent(player.getOpenInventory()));
            scheduler.runDeferred(); // the scheduled reopen runs

            assertThat(gui.getInventory().getViewers()).contains(player);
        } finally {
            Guis.uninstall();
        }
    }

    @Test
    void deliberateCloseStillClosesAPreventCloseMenu() {
        CapturingScheduler scheduler = new CapturingScheduler();
        Guis.install(MockBukkit.createMockPlugin(), scheduler);
        try {
            SimpleGui gui = Guis.gui().rows(1).build();
            gui.preventClose(true);
            int[] closes = {0};
            gui.onClose(e -> closes[0]++);
            PlayerMock player = server.addPlayer();
            gui.open(player);

            // An API-driven close must not be intercepted by the reopen guard. closeInventory fires the real
            // InventoryCloseEvent, which the installed listener routes to handleClose while the
            // programmatic-close flag is set, so the close handler runs and nothing is rescheduled.
            gui.close(player);
            scheduler.runDeferred(); // no reopen should have been queued

            assertThat(closes[0]).isEqualTo(1);
            assertThat(gui.getInventory().getViewers()).doesNotContain(player);
        } finally {
            Guis.uninstall();
        }
    }
}
