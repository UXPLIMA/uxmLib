package com.uxplima.uxmlib.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Pure tests for the animated slot-pattern engine: frame advance on a tick count, the changed-slot diff
 * (only the slots that differ between two frames), and the border-ring / sweep generators. No live
 * inventory: these are side-effect-free so the frame maths is unit-testable on its own.
 */
class SlotPatternTest {

    @Test
    void framesWrapWithTheTickCount() {
        SlotPattern pattern = SlotPattern.of(List.of(List.of(0), List.of(1), List.of(2)));

        assertThat(pattern.frameCount()).isEqualTo(3);
        assertThat(pattern.frameIndexAt(0L)).isZero();
        assertThat(pattern.frameIndexAt(1L)).isEqualTo(1);
        assertThat(pattern.frameIndexAt(3L)).isZero(); // wraps
        assertThat(pattern.frameIndexAt(4L)).isEqualTo(1);
    }

    @Test
    void slotsAtReturnsTheLitSlotsForTheTick() {
        SlotPattern pattern = SlotPattern.of(List.of(List.of(0, 1), List.of(8)));

        assertThat(pattern.slotsAt(0L)).containsExactlyInAnyOrder(0, 1);
        assertThat(pattern.slotsAt(1L)).containsExactly(8);
    }

    @Test
    void diffReportsOnlyTheChangedSlots() {
        SlotPattern pattern = SlotPattern.of(List.of(List.of(0, 1, 2), List.of(1, 2, 3)));

        SlotPattern.FrameDiff diff = pattern.diff(0, 1);

        assertThat(diff.toClear()).containsExactly(0); // in the old frame, not the new
        assertThat(diff.toLight()).containsExactlyInAnyOrder(1, 2, 3); // every slot in the new frame
    }

    @Test
    void diffOfTheSameFrameClearsNothing() {
        SlotPattern pattern = SlotPattern.of(List.of(List.of(4, 5)));

        SlotPattern.FrameDiff diff = pattern.diff(0, 0);

        assertThat(diff.toClear()).isEmpty();
        assertThat(diff.toLight()).containsExactlyInAnyOrder(4, 5);
    }

    @Test
    void clockwiseBorderWalksOneSlotPerFrameAroundTheEdge() {
        // A 3x3 grid: the ordered border is 0,1,2,5,8,7,6,3 (top row, right col, bottom row reversed, left up).
        SlotPattern pattern = SlotPattern.clockwiseBorder(3, 3, 1, 0);

        assertThat(pattern.frameCount()).isEqualTo(8); // eight border slots
        assertThat(pattern.frame(0)).containsExactly(0);
        assertThat(pattern.frame(1)).containsExactly(1);
        assertThat(pattern.frame(2)).containsExactly(2);
        assertThat(pattern.frame(3)).containsExactly(5);
        assertThat(pattern.frame(4)).containsExactly(8);
        assertThat(pattern.frame(7)).containsExactly(3);
    }

    @Test
    void clockwiseBorderLightsSeveralAdjacentSlotsWithAWindow() {
        SlotPattern pattern = SlotPattern.clockwiseBorder(3, 3, 3, 0);

        // A three-wide window starting at each border position, wrapping around the ring.
        assertThat(pattern.frame(0)).containsExactly(0, 1, 2);
        assertThat(pattern.frame(7)).containsExactly(3, 0, 1); // wraps past the end of the ring
    }

    @Test
    void clockwiseBorderOffsetRotatesTheStart() {
        SlotPattern base = SlotPattern.clockwiseBorder(3, 3, 1, 0);
        SlotPattern shifted = SlotPattern.clockwiseBorder(3, 3, 1, 2);

        // An offset of two starts the walk two slots further along the ring.
        assertThat(shifted.frame(0)).isEqualTo(base.frame(2));
    }

    @Test
    void sweepLightsAWholeColumnPerFrame() {
        SlotPattern pattern = SlotPattern.sweep(3, 2); // 3 wide, 2 tall

        assertThat(pattern.frameCount()).isEqualTo(3); // one frame per column
        assertThat(pattern.frame(0)).containsExactlyInAnyOrder(0, 3); // column 0: slots 0 and 3
        assertThat(pattern.frame(1)).containsExactlyInAnyOrder(1, 4);
        assertThat(pattern.frame(2)).containsExactlyInAnyOrder(2, 5);
    }

    @Test
    void orderedBorderSlotsAreClockwiseFromTheTopLeft() {
        List<Integer> ring = SlotPattern.orderedBorderSlots(3, 3, 0);
        assertThat(ring).containsExactly(0, 1, 2, 5, 8, 7, 6, 3);
    }

    @Test
    void rejectsEmptyFrames() {
        assertThatThrownBy(() -> SlotPattern.of(List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBadDimensions() {
        assertThatThrownBy(() -> SlotPattern.clockwiseBorder(0, 3, 1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SlotPattern.sweep(3, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
