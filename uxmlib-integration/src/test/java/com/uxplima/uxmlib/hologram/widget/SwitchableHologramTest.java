package com.uxplima.uxmlib.hologram.widget;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Drives the switchable widget: a viewer is shown whichever state's condition passes first, refreshing to a
 * different state hides the old one and shows the new (only that viewer), an unchanged state is a no-op, and
 * a viewer matching no state sees nothing. The stat lookup is controllable so a refresh can flip a viewer
 * between tiers without any server.
 */
class SwitchableHologramTest {

    private static final UUID A = new UUID(0, 1);
    private static final UUID B = new UUID(0, 2);

    /** Records which (viewer, state-value) pairs were shown and hidden. */
    private static final class RecordingStates implements StatePresenter<String> {
        private final List<String> shown = new ArrayList<>();
        private final List<String> hidden = new ArrayList<>();

        @Override
        public void show(String state, UUID viewer) {
            shown.add(state + ":" + viewer);
        }

        @Override
        public void hide(String state, UUID viewer) {
            hidden.add(state + ":" + viewer);
        }

        void clear() {
            shown.clear();
            hidden.clear();
        }
    }

    private static SwitchSelection<String> tieredSelection() {
        return new SwitchSelection<>(List.of(
                new SwitchSelection.State<>(c -> c.stat("rank") >= 100, "admin"),
                new SwitchSelection.State<>(c -> c.stat("rank") >= 10, "vip"),
                new SwitchSelection.State<>(c -> c.stat("rank") >= 0, "member")));
    }

    private static ViewerContext ctx(UUID viewer, double rank) {
        return new ViewerContext(viewer, stat -> rank);
    }

    @Test
    void refreshShowsTheFirstMatchingState() {
        RecordingStates states = new RecordingStates();
        SwitchableHologram<String> widget = new SwitchableHologram<>(tieredSelection(), states);

        widget.refresh(ctx(A, 50)); // matches vip (skips admin)

        assertThat(states.shown).containsExactly("vip:" + A);
        assertThat(states.hidden).isEmpty();
    }

    @Test
    void crossingAThresholdHidesTheOldStateAndShowsTheNewForThatViewerOnly() {
        RecordingStates states = new RecordingStates();
        SwitchableHologram<String> widget = new SwitchableHologram<>(tieredSelection(), states);
        widget.refresh(ctx(A, 5)); // member
        widget.refresh(ctx(B, 5)); // member
        states.clear();

        widget.refresh(ctx(A, 200)); // member -> admin

        assertThat(states.hidden).containsExactly("member:" + A);
        assertThat(states.shown).containsExactly("admin:" + A); // B untouched
    }

    @Test
    void reRenderingTheSameStateIsANoOp() {
        RecordingStates states = new RecordingStates();
        SwitchableHologram<String> widget = new SwitchableHologram<>(tieredSelection(), states);
        widget.refresh(ctx(A, 50));
        states.clear();

        widget.refresh(ctx(A, 60)); // still vip

        assertThat(states.shown).isEmpty();
        assertThat(states.hidden).isEmpty();
    }

    @Test
    void noMatchHidesAnyPreviousStateAndShowsNothing() {
        RecordingStates states = new RecordingStates();
        SwitchSelection<String> adminOnly =
                new SwitchSelection<>(List.of(new SwitchSelection.State<>(c -> c.stat("rank") >= 100, "admin")));
        SwitchableHologram<String> widget = new SwitchableHologram<>(adminOnly, states);
        widget.refresh(ctx(A, 200)); // admin
        states.clear();

        widget.refresh(ctx(A, 1)); // no state matches now

        assertThat(states.hidden).containsExactly("admin:" + A);
        assertThat(states.shown).isEmpty();
    }

    @Test
    void quitHidesTheViewersStateAndForgetsIt() {
        RecordingStates states = new RecordingStates();
        SwitchableHologram<String> widget = new SwitchableHologram<>(tieredSelection(), states);
        widget.refresh(ctx(A, 50)); // vip
        states.clear();

        widget.onQuit(A);
        assertThat(states.hidden).containsExactly("vip:" + A);

        states.clear();
        widget.refresh(ctx(A, 50)); // re-render: treated as fresh, shows again with no spurious hide
        assertThat(states.hidden).isEmpty();
        assertThat(states.shown).containsExactly("vip:" + A);
    }
}
