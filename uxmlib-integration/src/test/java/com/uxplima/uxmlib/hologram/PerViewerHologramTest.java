package com.uxplima.uxmlib.hologram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import net.kyori.adventure.text.Component;

import com.uxplima.uxmlib.text.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

/** Per-viewer dynamic text: each player sees their own rendered line off one entity per viewer. */
class PerViewerHologramTest {

    private ServerMock server;
    private WorldMock world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /** A spawn function that records the entities it made so the test can read their text back. */
    private static final class RecordingSpawner implements Function<Location, TextDisplay> {
        private final WorldMock world;
        final List<TextDisplay> spawned = new ArrayList<>();

        RecordingSpawner(WorldMock world) {
            this.world = world;
        }

        @Override
        public TextDisplay apply(Location location) {
            TextDisplay display = world.spawn(location, TextDisplay.class);
            spawned.add(display);
            return display;
        }
    }

    @Test
    void rendersEachViewersOwnTextFromTheFunction() {
        var plugin = MockBukkit.createMockPlugin();
        Player alice = server.addPlayer("Alice");
        Player bob = server.addPlayer("Bob");
        RecordingSpawner spawner = new RecordingSpawner(world);
        PerViewerHologram hologram = new PerViewerHologram(spawner, new Location(world, 0, 64, 0));
        hologram.setText(player -> Component.text("hello " + player.getName()));

        hologram.update(plugin, alice);
        hologram.update(plugin, bob);

        // Two viewers, two private entities, each carrying that viewer's own text.
        assertThat(spawner.spawned).hasSize(2);
        assertThat(Text.plain(spawner.spawned.get(0).text())).isEqualTo("hello Alice");
        assertThat(Text.plain(spawner.spawned.get(1).text())).isEqualTo("hello Bob");
    }

    @Test
    void updateRefreshesAnExistingViewerWithoutSpawningAgain() {
        var plugin = MockBukkit.createMockPlugin();
        Player alice = server.addPlayer("Alice");
        int[] tick = {0};
        RecordingSpawner spawner = new RecordingSpawner(world);
        PerViewerHologram hologram = new PerViewerHologram(spawner, new Location(world, 0, 64, 0));
        hologram.setText(player -> Component.text("tick " + tick[0]));

        hologram.update(plugin, alice);
        tick[0] = 5;
        hologram.update(plugin, alice);

        assertThat(spawner.spawned).hasSize(1); // re-used, not re-spawned
        assertThat(Text.plain(spawner.spawned.get(0).text())).isEqualTo("tick 5");
    }

    @Test
    void updateAllRefreshesEveryActiveViewer() {
        var plugin = MockBukkit.createMockPlugin();
        Player alice = server.addPlayer("Alice");
        Player bob = server.addPlayer("Bob");
        int[] tick = {1};
        RecordingSpawner spawner = new RecordingSpawner(world);
        PerViewerHologram hologram = new PerViewerHologram(spawner, new Location(world, 0, 64, 0));
        hologram.setText(player -> Component.text(player.getName() + "-" + tick[0]));
        hologram.update(plugin, alice);
        hologram.update(plugin, bob);

        tick[0] = 2;
        hologram.updateAll();

        assertThat(Text.plain(spawner.spawned.get(0).text())).isEqualTo("Alice-2");
        assertThat(Text.plain(spawner.spawned.get(1).text())).isEqualTo("Bob-2");
    }

    @Test
    void removeForViewerDespawnsThatViewersEntity() {
        var plugin = MockBukkit.createMockPlugin();
        Player alice = server.addPlayer("Alice");
        RecordingSpawner spawner = new RecordingSpawner(world);
        PerViewerHologram hologram = new PerViewerHologram(spawner, new Location(world, 0, 64, 0));
        hologram.setText(player -> Component.text("x"));
        hologram.update(plugin, alice);

        hologram.removeViewer(alice);

        assertThat(spawner.spawned.get(0).isDead()).isTrue();
        assertThat(hologram.viewerCount()).isZero();
    }

    @Test
    void removeDespawnsEveryViewersEntity() {
        var plugin = MockBukkit.createMockPlugin();
        Player alice = server.addPlayer("Alice");
        Player bob = server.addPlayer("Bob");
        RecordingSpawner spawner = new RecordingSpawner(world);
        PerViewerHologram hologram = new PerViewerHologram(spawner, new Location(world, 0, 64, 0));
        hologram.setText(player -> Component.text("x"));
        hologram.update(plugin, alice);
        hologram.update(plugin, bob);

        hologram.remove();

        assertThat(spawner.spawned).allMatch(TextDisplay::isDead);
        assertThat(hologram.viewerCount()).isZero();
    }

    @Test
    void updateBeforeSetTextThrows() {
        var plugin = MockBukkit.createMockPlugin();
        Player alice = server.addPlayer("Alice");
        PerViewerHologram hologram = new PerViewerHologram(new RecordingSpawner(world), new Location(world, 0, 64, 0));
        assertThatThrownBy(() -> hologram.update(plugin, alice)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsANullTextFunction() {
        PerViewerHologram hologram = new PerViewerHologram(new RecordingSpawner(world), new Location(world, 0, 64, 0));
        assertThatThrownBy(() -> hologram.setText(null)).isInstanceOf(NullPointerException.class);
    }
}
