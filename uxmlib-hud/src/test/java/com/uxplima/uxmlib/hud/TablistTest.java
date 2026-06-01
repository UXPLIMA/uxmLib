package com.uxplima.uxmlib.hud;

import static org.assertj.core.api.Assertions.assertThatCode;

import net.kyori.adventure.text.Component;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Smoke test for the tablist helper. Every method is a thin pass-through to the Adventure
 * {@code sendPlayerListHeaderAndFooter}; MockBukkit does not route that default into its String header
 * store, so we assert the wiring fires cleanly rather than reading the header back.
 */
class TablistTest {

    private ServerMock server;
    private final Tablist tablist = new Tablist();

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void setHeaderFooterDoesNotThrow() {
        PlayerMock player = server.addPlayer();
        assertThatCode(() -> tablist.set(player, Component.text("Header"), Component.text("Footer")))
                .doesNotThrowAnyException();
    }

    @Test
    void headerOnlyAndFooterOnlyDoNotThrow() {
        PlayerMock player = server.addPlayer();
        assertThatCode(() -> tablist.header(player, Component.text("H"))).doesNotThrowAnyException();
        assertThatCode(() -> tablist.footer(player, Component.text("F"))).doesNotThrowAnyException();
        assertThatCode(() -> tablist.clear(player)).doesNotThrowAnyException();
    }
}
