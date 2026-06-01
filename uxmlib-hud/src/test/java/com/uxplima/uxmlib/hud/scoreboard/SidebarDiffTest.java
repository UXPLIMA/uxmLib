package com.uxplima.uxmlib.hud.scoreboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import net.kyori.adventure.text.Component;

import org.junit.jupiter.api.Test;

/** Directly tests the flicker-free diff: only changed lines move, with explicit add/remove of the tail. */
class SidebarDiffTest {

    private static Component c(String s) {
        return Component.text(s);
    }

    @Test
    void identicalListsProduceNoChanges() {
        List<Component> lines = List.of(c("a"), c("b"), c("c"));
        SidebarDiff.Plan plan = SidebarDiff.diff(lines, lines);
        assertThat(plan.isEmpty()).isTrue();
    }

    @Test
    void onlyTheChangedLineIsReported() {
        List<Component> previous = List.of(c("a"), c("b"), c("c"));
        List<Component> next = List.of(c("a"), c("X"), c("c"));
        SidebarDiff.Plan plan = SidebarDiff.diff(previous, next);
        assertThat(plan.changed()).containsExactly(1);
        assertThat(plan.added()).isEmpty();
        assertThat(plan.removed()).isEmpty();
    }

    @Test
    void growingTheBoardAddsTrailingIndices() {
        List<Component> previous = List.of(c("a"));
        List<Component> next = List.of(c("a"), c("b"), c("c"));
        SidebarDiff.Plan plan = SidebarDiff.diff(previous, next);
        assertThat(plan.changed()).isEmpty();
        assertThat(plan.added()).containsExactly(1, 2);
        assertThat(plan.removed()).isEmpty();
    }

    @Test
    void shrinkingTheBoardRemovesTrailingIndices() {
        List<Component> previous = List.of(c("a"), c("b"), c("c"));
        List<Component> next = List.of(c("a"));
        SidebarDiff.Plan plan = SidebarDiff.diff(previous, next);
        assertThat(plan.changed()).isEmpty();
        assertThat(plan.added()).isEmpty();
        assertThat(plan.removed()).containsExactly(1, 2);
    }

    @Test
    void aShrinkThatAlsoEditsAKeptLineReportsBoth() {
        List<Component> previous = List.of(c("a"), c("b"), c("c"));
        List<Component> next = List.of(c("Z"), c("b"));
        SidebarDiff.Plan plan = SidebarDiff.diff(previous, next);
        assertThat(plan.changed()).containsExactly(0);
        assertThat(plan.removed()).containsExactly(2);
        assertThat(plan.added()).isEmpty();
    }

    @Test
    void everyLineKeyIsUniqueAcrossTheMaximum() {
        long distinct = java.util.stream.IntStream.range(0, Sidebar.MAX_LINES)
                .mapToObj(Sidebar::entryKey)
                .distinct()
                .count();
        assertThat(distinct).isEqualTo(Sidebar.MAX_LINES);
    }
}
