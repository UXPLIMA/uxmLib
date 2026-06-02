package com.uxplima.uxmlib.hologram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

/**
 * Tests the managed-as-one mixed hologram: removal, restriction, and per-viewer visibility fan out across
 * every part. The parts are real {@code TextDisplay}s spawned via MockBukkit (without the appearance pass,
 * which MockBukkit's display does not fully implement), so {@link MixedHologram}'s own fan-out logic runs
 * against live entities.
 */
class MixedHologramTest {

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

    private MixedHologram twoParts() {
        Display a = world.spawn(new Location(world, 0, 64, 0), TextDisplay.class);
        Display b = world.spawn(new Location(world, 0, 63, 0), TextDisplay.class);
        return new MixedHologram(List.of(a, b));
    }

    @Test
    void keepsItsPartsInOrder() {
        Display a = world.spawn(new Location(world, 0, 64, 0), TextDisplay.class);
        Display b = world.spawn(new Location(world, 0, 63, 0), TextDisplay.class);
        assertThat(new MixedHologram(List.of(a, b)).parts()).containsExactly(a, b);
    }

    @Test
    void removeDespawnsEveryPart() {
        MixedHologram hologram = twoParts();

        hologram.remove();

        assertThat(hologram.parts()).allMatch(Display::isDead);
    }

    @Test
    void showTracksTheViewerAcrossEveryPart() {
        var plugin = MockBukkit.createMockPlugin();
        Player viewer = server.addPlayer("Alice");
        MixedHologram hologram = twoParts();

        hologram.show(plugin, viewer);

        assertThat(hologram.isVisibleTo(viewer)).isTrue();
    }

    @Test
    void hideUntracksTheViewer() {
        var plugin = MockBukkit.createMockPlugin();
        Player viewer = server.addPlayer("Alice");
        MixedHologram hologram = twoParts();
        hologram.show(plugin, viewer);

        hologram.hide(plugin, viewer);

        assertThat(hologram.isVisibleTo(viewer)).isFalse();
    }

    @Test
    void forgetViewerDropsTheTrackedUuidWithoutAnyPacket() {
        var plugin = MockBukkit.createMockPlugin();
        Player viewer = server.addPlayer("Alice");
        MixedHologram hologram = twoParts();
        hologram.show(plugin, viewer);

        hologram.forgetViewer(viewer.getUniqueId());

        assertThat(hologram.isVisibleTo(viewer)).isFalse();
    }

    @Test
    void rejectsAnEmptyPartList() {
        assertThatThrownBy(() -> new MixedHologram(List.of())).isInstanceOf(IllegalArgumentException.class);
    }
}
