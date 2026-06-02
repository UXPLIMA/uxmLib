package com.uxplima.uxmlib.hologram.widget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Pure tests of the per-player page state machine: the current page defaults to zero, advancing and
 * retreating wrap around the page count, and each player's page is isolated from every other player's. No
 * server or entity is touched.
 */
class PageStateTest {

    private static final UUID A = new UUID(0, 1);
    private static final UUID B = new UUID(0, 2);

    @Test
    void defaultsToFirstPage() {
        PageState state = new PageState(3);
        assertThat(state.current(A)).isZero();
    }

    @Test
    void nextAdvancesAndReturnsTheNewPage() {
        PageState state = new PageState(3);
        assertThat(state.next(A)).isEqualTo(1);
        assertThat(state.next(A)).isEqualTo(2);
        assertThat(state.current(A)).isEqualTo(2);
    }

    @Test
    void nextWrapsPastTheLastPageBackToTheFirst() {
        PageState state = new PageState(2);
        state.next(A); // -> 1 (last)
        assertThat(state.next(A)).isZero(); // wraps to 0
    }

    @Test
    void prevWrapsBeforeTheFirstPageToTheLast() {
        PageState state = new PageState(3);
        assertThat(state.prev(A)).isEqualTo(2); // 0 wraps to last
        assertThat(state.prev(A)).isEqualTo(1);
    }

    @Test
    void perPlayerPagesAreIsolated() {
        PageState state = new PageState(4);
        state.next(A); // A -> 1
        state.next(A); // A -> 2
        assertThat(state.current(A)).isEqualTo(2);
        assertThat(state.current(B)).isZero(); // B untouched
    }

    @Test
    void forgetResetsAPlayerToTheFirstPage() {
        PageState state = new PageState(3);
        state.next(A);
        state.next(A);
        state.forget(A);
        assertThat(state.current(A)).isZero();
    }

    @Test
    void setClampsIntoRange() {
        PageState state = new PageState(3);
        assertThat(state.set(A, 5)).isEqualTo(2); // clamped to last
        assertThat(state.set(A, -3)).isZero(); // clamped to first
        assertThat(state.set(A, 1)).isEqualTo(1);
    }

    @Test
    void singlePageStaysOnPageZeroThroughNextAndPrev() {
        PageState state = new PageState(1);
        assertThat(state.next(A)).isZero();
        assertThat(state.prev(A)).isZero();
    }

    @Test
    void rejectsNonPositivePageCount() {
        assertThatThrownBy(() -> new PageState(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageState(-2)).isInstanceOf(IllegalArgumentException.class);
    }
}
