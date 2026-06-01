package com.uxplima.uxmlib.hud.scoreboard;

import static org.assertj.core.api.Assertions.assertThat;

import org.bukkit.event.player.PlayerQuitEvent;

import net.kyori.adventure.text.Component;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** Verifies the quit listener drops a departing player's sidebar so the manager does not leak it. */
class SidebarListenerTest {

    private ServerMock server;
    private SidebarManager manager;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        manager = new SidebarManager(server.getScoreboardManager());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void quittingForgetsTheSidebar() {
        PlayerMock player = server.addPlayer();
        manager.create(player, Component.text("Title"));
        SidebarListener listener = new SidebarListener(manager);

        listener.onQuit(new PlayerQuitEvent(player, Component.empty(), PlayerQuitEvent.QuitReason.DISCONNECTED));

        assertThat(manager.count()).isZero();
        assertThat(manager.get(player.getUniqueId())).isNull();
    }
}
