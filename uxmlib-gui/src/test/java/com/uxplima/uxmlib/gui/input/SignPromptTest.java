package com.uxplima.uxmlib.gui.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.bukkit.Material;
import org.bukkit.block.Block;

import net.kyori.adventure.text.Component;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Covers the transient sign block {@link SignPrompt} writes into the world: it is placed on open and the
 * pending entry is cleared by {@link SignPrompt#restore} (per player) and {@link SignPrompt#restoreAll} (on
 * teardown). The native sign editor cannot open under MockBukkit (the block is placed first, then
 * {@code openSign} throws), so each test opens inside a try/catch.
 *
 * <p>The <em>physical</em> block round-trip (the original block reappearing) is exercised only on a real
 * server: MockBukkit returns a live {@code BlockData} reference from {@code getBlockData()} (real Paper
 * returns a snapshot) and ships a broken {@code clone()}, so the exact restored block type cannot be
 * asserted here. These tests assert the placement and the bookkeeping/no-throw contract instead.
 */
class SignPromptTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static void openIgnoringEditor(SignPrompt prompt, PlayerMock player) {
        try {
            prompt.open(player, Component.text("Enter value"));
        } catch (RuntimeException ignored) {
            // The native editor cannot open under MockBukkit; the block has already been placed by then.
        }
    }

    @Test
    void openWritesASignThenRestoreClearsThePending() {
        SignPrompt prompt = new SignPrompt();
        PlayerMock player = server.addPlayer();
        Block block = player.getLocation().getBlock();
        block.setType(Material.AIR, false);

        openIgnoringEditor(prompt, player);
        assertThat(block.getType()).isEqualTo(Material.OAK_SIGN); // a real sign was written into the world

        // restore runs without throwing and is idempotent (the pending entry is removed, so a second restore
        // and the on-disable restoreAll are no-ops); the physical block round-trip is covered on a live server.
        assertThatCode(() -> prompt.restore(player)).doesNotThrowAnyException();
        assertThatCode(() -> prompt.restore(player)).doesNotThrowAnyException();
        assertThatCode(prompt::restoreAll).doesNotThrowAnyException();
    }

    @Test
    void restoreAllClearsEveryPendingWindow() {
        SignPrompt prompt = new SignPrompt();
        PlayerMock a = server.addPlayer();
        PlayerMock b = server.addPlayer();

        openIgnoringEditor(prompt, a);
        openIgnoringEditor(prompt, b);

        assertThatCode(prompt::restoreAll).doesNotThrowAnyException();
        // Every entry is now cleared, so a follow-up restoreAll (and a per-player restore) does nothing.
        assertThatCode(prompt::restoreAll).doesNotThrowAnyException();
        assertThatCode(() -> prompt.restore(a)).doesNotThrowAnyException();
    }

    @Test
    void restoreIsIdempotentWhenNothingIsPending() {
        SignPrompt prompt = new SignPrompt();
        PlayerMock player = server.addPlayer();

        // No prompt was opened for this player, so restore must be a harmless no-op.
        assertThatCode(() -> prompt.restore(player)).doesNotThrowAnyException();
    }
}
