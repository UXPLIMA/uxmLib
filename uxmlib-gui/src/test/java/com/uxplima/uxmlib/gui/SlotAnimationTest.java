package com.uxplima.uxmlib.gui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/**
 * Covers the moving-highlight overlay: that advancing the animation writes only the changed slots (the
 * diff), that an unchanged tick writes nothing, and that the guarded clear never clobbers a slot a caller
 * placed an item into.
 */
class SlotAnimationTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static ItemStack highlight() {
        return new ItemStack(Material.LIME_STAINED_GLASS_PANE);
    }

    /** Records the slots written/cleared so a test can assert exactly which slots a tick touched. */
    private static final class RecordingSink implements SlotAnimation.Sink {
        final List<Integer> lit = new ArrayList<>();
        final List<Integer> cleared = new ArrayList<>();
        private final java.util.Set<Integer> occupied;

        RecordingSink(Integer... occupiedSlots) {
            this.occupied = new java.util.HashSet<>(List.of(occupiedSlots));
        }

        @Override
        public boolean isFree(int slot) {
            return !occupied.contains(slot);
        }

        @Override
        public void light(int slot, ItemStack icon) {
            lit.add(slot);
            occupied.add(slot);
        }

        @Override
        public void clear(int slot) {
            cleared.add(slot);
            occupied.remove(slot);
        }
    }

    @Test
    void firstAdvanceLightsTheStartingFrame() {
        SlotAnimation animation = SlotAnimation.of(SlotPattern.clockwiseBorder(3, 3, 1, 0), highlight());
        RecordingSink sink = new RecordingSink();

        animation.advance(0L, sink);

        assertThat(sink.lit).containsExactly(0); // first border slot
        assertThat(sink.cleared).isEmpty();
    }

    @Test
    void advanceClearsTheOldSlotAndLightsTheNew() {
        SlotAnimation animation = SlotAnimation.of(SlotPattern.clockwiseBorder(3, 3, 1, 0), highlight());
        RecordingSink sink = new RecordingSink();

        animation.advance(0L, sink); // lights slot 0
        sink.lit.clear();
        animation.advance(1L, sink); // moves to slot 1

        assertThat(sink.cleared).containsExactly(0); // old highlight removed
        assertThat(sink.lit).containsExactly(1); // new highlight placed
    }

    @Test
    void anUnchangedTickWritesNothing() {
        SlotAnimation animation = SlotAnimation.of(SlotPattern.clockwiseBorder(3, 3, 1, 0), highlight());
        RecordingSink sink = new RecordingSink();

        animation.advance(0L, sink); // frame 0
        sink.lit.clear();
        animation.advance(0L, sink); // same tick -> same frame, no change

        assertThat(sink.lit).isEmpty();
        assertThat(sink.cleared).isEmpty();
    }

    @Test
    void guardedClearLeavesAnOverlaidButtonAlone() {
        SlotAnimation animation = SlotAnimation.of(SlotPattern.clockwiseBorder(3, 3, 1, 0), highlight());
        // Slot 0 already holds a caller's button; the animation must not light it nor later clear it.
        RecordingSink sink = new RecordingSink(0);

        animation.advance(0L, sink); // wants slot 0 but it is occupied -> skip
        assertThat(sink.lit).isEmpty();

        animation.advance(1L, sink); // moves to slot 1, must not clear slot 0 it never lit
        assertThat(sink.cleared).isEmpty();
        assertThat(sink.lit).containsExactly(1);
    }
}
