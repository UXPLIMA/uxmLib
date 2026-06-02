package com.uxplima.uxmlib.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MapMergeTest {

    @Test
    void userValueWinsForSharedKeys() {
        Map<String, Integer> managed = Map.of("a", 1, "b", 2);
        Map<String, Integer> user = Map.of("b", 99);

        Map<String, Integer> merged = MapMerge.userWins(managed, user);

        assertThat(merged).containsEntry("a", 1).containsEntry("b", 99);
    }

    @Test
    void managedFillsGapsForKeysTheUserOmits() {
        Map<String, Integer> managed = Map.of("a", 1, "b", 2, "c", 3);
        Map<String, Integer> user = Map.of("b", 99);

        Map<String, Integer> merged = MapMerge.userWins(managed, user);

        assertThat(merged).containsEntry("a", 1).containsEntry("c", 3);
    }

    @Test
    void userOnlyKeysAreKept() {
        Map<String, Integer> managed = Map.of("a", 1);
        Map<String, Integer> user = Map.of("z", 26);

        assertThat(MapMerge.userWins(managed, user)).containsEntry("z", 26);
    }

    @Test
    void managedKeysComeFirstThenExtraUserKeys() {
        Map<String, Integer> managed = new LinkedHashMap<>();
        managed.put("a", 1);
        managed.put("b", 2);
        Map<String, Integer> user = new LinkedHashMap<>();
        user.put("b", 99);
        user.put("z", 26);

        assertThat(MapMerge.userWins(managed, user).keySet()).containsExactly("a", "b", "z");
    }

    @Test
    void doesNotMutateEitherInput() {
        Map<String, Integer> managed = new LinkedHashMap<>(Map.of("a", 1));
        Map<String, Integer> user = new LinkedHashMap<>(Map.of("a", 2));

        MapMerge.userWins(managed, user);

        assertThat(managed).containsExactlyEntriesOf(Map.of("a", 1));
        assertThat(user).containsExactlyEntriesOf(Map.of("a", 2));
    }
}
