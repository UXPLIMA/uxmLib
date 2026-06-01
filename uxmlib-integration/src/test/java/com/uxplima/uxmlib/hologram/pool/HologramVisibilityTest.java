package com.uxplima.uxmlib.hologram.pool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * Exercises the pure visibility math the pool diffs on: the same-world/in-range predicate (MockBukkit
 * worlds and locations) and the set-difference that turns a desired viewer set into show/hide deltas
 * (plain UUID sets, no Bukkit needed).
 */
class HologramVisibilityTest {

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
    void showsWhenSameWorldAndWithinRadius() {
        World world = server.addSimpleWorld("pool");
        Location viewer = new Location(world, 0, 64, 0);
        Location holo = new Location(world, 6, 64, 8); // 36 + 64 = 100 <= 10^2
        assertThat(HologramVisibility.shouldShow(viewer, holo, 100.0)).isTrue();
    }

    @Test
    void hidesWhenBeyondRadius() {
        World world = server.addSimpleWorld("pool");
        Location viewer = new Location(world, 0, 64, 0);
        Location holo = new Location(world, 9, 64, 9); // 81 + 81 = 162 > 100
        assertThat(HologramVisibility.shouldShow(viewer, holo, 100.0)).isFalse();
    }

    @Test
    void hidesAcrossDifferentWorldsWithoutThrowing() {
        World a = server.addSimpleWorld("a");
        World b = server.addSimpleWorld("b");
        Location viewer = new Location(a, 0, 64, 0);
        Location holo = new Location(b, 0, 64, 0); // identical coords, other world
        // distanceSquared throws across worlds, so the world check must short-circuit.
        assertThat(HologramVisibility.shouldShow(viewer, holo, 100.0)).isFalse();
    }

    @Test
    void hidesWhenAWorldIsMissing() {
        Location viewer = new Location(null, 0, 64, 0);
        Location holo = new Location(null, 1, 64, 1);
        assertThat(HologramVisibility.shouldShow(viewer, holo, 100.0)).isFalse();
    }

    @Test
    void toShowIsDesiredMinusCurrent() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        Set<UUID> current = Set.of(a, b);
        Set<UUID> desired = Set.of(b, c);
        assertThat(HologramVisibility.toShow(current, desired)).containsExactly(c);
    }

    @Test
    void toHideIsCurrentMinusDesired() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        Set<UUID> current = Set.of(a, b);
        Set<UUID> desired = Set.of(b, c);
        assertThat(HologramVisibility.toHide(current, desired)).containsExactly(a);
    }

    @Test
    void noDeltaWhenSetsMatch() {
        UUID a = UUID.randomUUID();
        Set<UUID> current = Set.of(a);
        Set<UUID> desired = Set.of(a);
        assertThat(HologramVisibility.toShow(current, desired)).isEmpty();
        assertThat(HologramVisibility.toHide(current, desired)).isEmpty();
    }
}
