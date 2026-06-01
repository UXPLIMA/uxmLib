package com.uxplima.uxmlib.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class ServerVersionTest {

    @Test
    void parsesMajorMinorPatch() {
        ServerVersion version = ServerVersion.parse("1.21.4");
        assertThat(version.major()).isEqualTo(1);
        assertThat(version.minor()).isEqualTo(21);
        assertThat(version.patch()).isEqualTo(4);
    }

    @Test
    void treatsMissingPatchAsZero() {
        assertThat(ServerVersion.parse("1.21")).isEqualTo(ServerVersion.of(1, 21, 0));
    }

    @Test
    void ignoresTrailingQualifier() {
        assertThat(ServerVersion.parse("1.21.11-R0.1-SNAPSHOT")).isEqualTo(ServerVersion.of(1, 21, 11));
        assertThat(ServerVersion.parse("1.21.4-pre2")).isEqualTo(ServerVersion.of(1, 21, 4));
    }

    @Test
    void rejectsGarbage() {
        assertThatThrownBy(() -> ServerVersion.parse("not-a-version")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ServerVersion.parse("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isAtLeastComparesLexicographically() {
        ServerVersion v = ServerVersion.of(1, 21, 4);
        assertThat(v.isAtLeast(1, 21, 4)).isTrue();
        assertThat(v.isAtLeast(1, 21, 3)).isTrue();
        assertThat(v.isAtLeast(1, 20, 6)).isTrue();
        assertThat(v.isAtLeast(1, 21, 5)).isFalse();
        assertThat(v.isAtLeast(1, 22, 0)).isFalse();
        assertThat(v.isAtLeast(2, 0, 0)).isFalse();
    }

    @Test
    void isAtLeastHandlesTheTwoArgPatchDefault() {
        ServerVersion v = ServerVersion.of(1, 21, 0);
        assertThat(v.isAtLeast(1, 21)).isTrue();
        assertThat(v.isAtLeast(1, 20)).isTrue();
        assertThat(v.isAtLeast(1, 22)).isFalse();
    }

    @Test
    void rejectsNegativeComponents() {
        assertThatThrownBy(() -> ServerVersion.of(-1, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ServerVersion.of(1, -1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ServerVersion.of(1, 0, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rendersAStableString() {
        assertThat(ServerVersion.of(1, 21, 4).toString()).isEqualTo("1.21.4");
    }

    // The running-server probe needs a mocked server; MockBukkit reports 1.21.x.
    @Nested
    class RunningProbe {

        @BeforeEach
        void setUp() {
            MockBukkit.mock();
        }

        @AfterEach
        void tearDown() {
            MockBukkit.unmock();
        }

        @Test
        void readsTheRunningVersionFromBukkit() {
            ServerVersion current = ServerVersion.current();
            assertThat(current.isAtLeast(1, 21, 0)).isTrue();
            assertThat(current.major()).isEqualTo(1);
        }

        @Test
        void cachesTheRunningVersion() {
            assertThat(ServerVersion.current()).isSameAs(ServerVersion.current());
        }

        @Test
        void notRunningUnderFolia() {
            // MockBukkit is plain Paper; the Folia marker class is absent.
            assertThat(ServerVersion.isFolia()).isFalse();
        }
    }
}
