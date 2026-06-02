package com.uxplima.uxmlib.hologram.follow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.bukkit.entity.Player;

import com.uxplima.uxmlib.hologram.RecordingHologram;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * The third-person-only nameplate: a follow hologram shown to nearby players but never to its own wearer,
 * using native per-viewer visibility (no packets).
 */
class SelfHiddenNameplateTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void restrictsTheHologramSoOnlyExplicitViewersSeeIt() {
        RecordingHologram hologram = new RecordingHologram();
        Player wearer = server.addPlayer("Wearer");

        new SelfHiddenNameplate(hologram, wearer);

        assertThat(hologram.restricted).isTrue();
    }

    @Test
    void showsToEveryNearbyPlayerExceptTheWearer() {
        var plugin = MockBukkit.createMockPlugin();
        RecordingHologram hologram = new RecordingHologram();
        Player wearer = server.addPlayer("Wearer");
        Player other = server.addPlayer("Other");
        Player third = server.addPlayer("Third");
        SelfHiddenNameplate nameplate = new SelfHiddenNameplate(hologram, wearer);

        nameplate.refreshViewers(plugin, List.of(wearer, other, third));

        assertThat(hologram.shown).containsExactlyInAnyOrder(other.getUniqueId(), third.getUniqueId());
        assertThat(hologram.shown).doesNotContain(wearer.getUniqueId());
    }

    @Test
    void hidesFromAnybodyWhoWalkedOutOfTheCandidateSet() {
        var plugin = MockBukkit.createMockPlugin();
        RecordingHologram hologram = new RecordingHologram();
        Player wearer = server.addPlayer("Wearer");
        Player other = server.addPlayer("Other");
        SelfHiddenNameplate nameplate = new SelfHiddenNameplate(hologram, wearer);
        nameplate.refreshViewers(plugin, List.of(other));

        // Next pass: 'other' is no longer nearby, so it must be hidden.
        nameplate.refreshViewers(plugin, List.of());

        assertThat(hologram.hidden).contains(other.getUniqueId());
    }

    @Test
    void neverShowsToTheWearerEvenIfTheyAreTheOnlyCandidate() {
        var plugin = MockBukkit.createMockPlugin();
        RecordingHologram hologram = new RecordingHologram();
        Player wearer = server.addPlayer("Wearer");
        SelfHiddenNameplate nameplate = new SelfHiddenNameplate(hologram, wearer);

        nameplate.refreshViewers(plugin, List.of(wearer));

        assertThat(hologram.shown).isEmpty();
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsANullWearer() {
        assertThatThrownBy(() -> new SelfHiddenNameplate(new RecordingHologram(), null))
                .isInstanceOf(NullPointerException.class);
    }
}
