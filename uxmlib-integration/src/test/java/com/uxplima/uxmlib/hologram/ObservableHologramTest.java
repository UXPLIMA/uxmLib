package com.uxplima.uxmlib.hologram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/** Per-hologram show/hide hooks fired on visibility change, wrapping any underlying hologram. */
class ObservableHologramTest {

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
    void firesOnShowWhenAViewerIsShown() {
        var plugin = MockBukkit.createMockPlugin();
        RecordingHologram delegate = new RecordingHologram();
        List<String> shown = new ArrayList<>();
        ObservableHologram hologram = ObservableHologram.wrapping(delegate).onShow(p -> shown.add(p.getName()));
        Player alice = server.addPlayer("Alice");

        hologram.show(plugin, alice);

        assertThat(shown).containsExactly("Alice");
        assertThat(delegate.shown).contains(alice.getUniqueId()); // delegated through
    }

    @Test
    void firesOnHideWhenAViewerIsHidden() {
        var plugin = MockBukkit.createMockPlugin();
        RecordingHologram delegate = new RecordingHologram();
        List<String> hidden = new ArrayList<>();
        ObservableHologram hologram = ObservableHologram.wrapping(delegate).onHide(p -> hidden.add(p.getName()));
        Player alice = server.addPlayer("Alice");

        hologram.show(plugin, alice);
        hologram.hide(plugin, alice);

        assertThat(hidden).containsExactly("Alice");
    }

    @Test
    void doesNotFireOnShowForAnAlreadyVisibleViewer() {
        var plugin = MockBukkit.createMockPlugin();
        RecordingHologram delegate = new RecordingHologram();
        List<String> shown = new ArrayList<>();
        ObservableHologram hologram = ObservableHologram.wrapping(delegate).onShow(p -> shown.add(p.getName()));
        Player alice = server.addPlayer("Alice");

        hologram.show(plugin, alice);
        hologram.show(plugin, alice); // already a viewer: no second callback

        assertThat(shown).containsExactly("Alice");
    }

    @Test
    void doesNotFireOnHideForANonViewer() {
        var plugin = MockBukkit.createMockPlugin();
        RecordingHologram delegate = new RecordingHologram();
        List<String> hidden = new ArrayList<>();
        ObservableHologram hologram = ObservableHologram.wrapping(delegate).onHide(p -> hidden.add(p.getName()));
        Player alice = server.addPlayer("Alice");

        hologram.hide(plugin, alice); // never shown: nothing to fire

        assertThat(hidden).isEmpty();
    }

    @Test
    void aBrokenHookNeverBreaksTheVisibilityChange() {
        var plugin = MockBukkit.createMockPlugin();
        RecordingHologram delegate = new RecordingHologram();
        ObservableHologram hologram = ObservableHologram.wrapping(delegate).onShow(p -> {
            throw new RuntimeException("boom");
        });
        Player alice = server.addPlayer("Alice");

        hologram.show(plugin, alice); // must not propagate the hook's exception

        assertThat(delegate.shown).contains(alice.getUniqueId());
    }

    @Test
    void supportsMultipleHooks() {
        var plugin = MockBukkit.createMockPlugin();
        RecordingHologram delegate = new RecordingHologram();
        List<String> a = new ArrayList<>();
        List<String> b = new ArrayList<>();
        ObservableHologram hologram = ObservableHologram.wrapping(delegate)
                .onShow(p -> a.add(p.getName()))
                .onShow(p -> b.add(p.getName()));
        Player alice = server.addPlayer("Alice");

        hologram.show(plugin, alice);

        assertThat(a).containsExactly("Alice");
        assertThat(b).containsExactly("Alice");
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsANullDelegate() {
        assertThatThrownBy(() -> ObservableHologram.wrapping(null)).isInstanceOf(NullPointerException.class);
    }
}
