package com.uxplima.uxmlib.hologram.widget;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;

import net.kyori.adventure.text.Component;

import com.uxplima.uxmlib.hologram.Hologram;
import com.uxplima.uxmlib.hologram.Transform;
import com.uxplima.uxmlib.scheduler.Scheduler;
import com.uxplima.uxmlib.scheduler.TaskHandle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * Smoke-tests the production presenters' wiring with MockBukkit: with a real online player and a hologram
 * whose native {@code show}/{@code hide} are recorded, driving the presenter and the paged widget through it
 * runs the show/hide on the (inline) entity scheduler without throwing. The page-state and selection logic is
 * covered purely elsewhere; this asserts the Bukkit-resolving glue holds together.
 */
class DisplayPresenterSmokeTest {

    private ServerMock server;
    private Plugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("widgets");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void pagePresenterShowsAndHidesAnOnlinePlayerWithoutThrowing() {
        Player viewer = server.addPlayer();
        UUID id = viewer.getUniqueId();
        RecordingHologram pageZero = new RecordingHologram();
        RecordingHologram pageOne = new RecordingHologram();
        DisplayPagePresenter presenter =
                new DisplayPagePresenter(List.of(pageZero, pageOne), plugin, new InlineScheduler());
        PagedHologram widget = new PagedHologram(2, presenter);

        assertThatCode(() -> {
                    widget.open(id); // shows page 0
                    widget.next(id); // hides page 0, shows page 1
                    widget.onQuit(id); // hides current page, resets
                })
                .doesNotThrowAnyException();
    }

    @Test
    void statePresenterSkipsAnUnknownStateValue() {
        Player viewer = server.addPlayer();
        RecordingHologram member = new RecordingHologram();
        DisplayStatePresenter<String> presenter =
                new DisplayStatePresenter<>(Map.of("member", member), plugin, new InlineScheduler());

        // "admin" has no mapped hologram: a no-op, not a throw.
        assertThatCode(() -> {
                    presenter.show("admin", viewer.getUniqueId());
                    presenter.show("member", viewer.getUniqueId());
                })
                .doesNotThrowAnyException();
    }

    /** A hologram backed by a mock TextDisplay; show/hide just need to not throw. */
    private static final class RecordingHologram implements Hologram {
        private final TextDisplay display = mock(TextDisplay.class);

        @Override
        public void setText(Component text) {}

        @Override
        public void moveTo(Location to, int interpolationTicks) {}

        @Override
        public void setTransform(Transform transform) {}

        @Override
        public boolean attachTo(Entity target) {
            return false;
        }

        @Override
        public void restrictToViewers() {}

        @Override
        public void show(Plugin plugin, Player viewer) {}

        @Override
        public void hide(Plugin plugin, Player viewer) {}

        @Override
        public boolean isVisibleTo(Player viewer) {
            return false;
        }

        @Override
        public void forgetViewer(UUID viewer) {}

        @Override
        public void remove() {}

        @Override
        public TextDisplay entity() {
            return display;
        }
    }

    /** A scheduler that runs entity tasks inline so the show/hide happens during the call. */
    private static final class InlineScheduler implements Scheduler {
        private final TaskHandle handle = new TaskHandle() {
            @Override
            public void cancel() {}

            @Override
            public boolean isCancelled() {
                return false;
            }
        };

        @Override
        public TaskHandle entity(Entity entity, Runnable task) {
            task.run();
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
}
