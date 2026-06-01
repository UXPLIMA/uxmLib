package com.uxplima.uxmlib.hud;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Duration;

import net.kyori.adventure.text.Component;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Smoke test for the title helper. MockBukkit does not round-trip the Adventure {@code showTitle(Title)}
 * path into its String title queue, so we assert the wiring fires cleanly rather than reading the title
 * back; the timing validation is asserted directly.
 */
class TitlesTest {

    private ServerMock server;
    private final Titles titles = new Titles();

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void showWithDefaultsDoesNotThrow() {
        PlayerMock player = server.addPlayer();
        assertThatCode(() -> titles.show(player, Component.text("Hi"), Component.text("there")))
                .doesNotThrowAnyException();
    }

    @Test
    void showWithCustomTimesDoesNotThrow() {
        PlayerMock player = server.addPlayer();
        assertThatCode(() -> titles.show(
                        player,
                        Component.text("Hi"),
                        Component.text("there"),
                        Duration.ofMillis(250),
                        Duration.ofSeconds(2),
                        Duration.ofMillis(250)))
                .doesNotThrowAnyException();
    }

    // clear()/reset() are thin pass-throughs to Player#clearTitle/#resetTitle; MockBukkit leaves those
    // unimplemented, so they cannot be round-tripped here. They are exercised against real Paper at runtime.

    @Test
    void negativeTimingIsRejected() {
        PlayerMock player = server.addPlayer();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> titles.show(
                        player,
                        Component.text("Hi"),
                        Component.text("there"),
                        Duration.ofMillis(-1),
                        Duration.ofSeconds(2),
                        Duration.ofMillis(250)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
