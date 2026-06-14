package com.uxplima.uxmlib.hud.scoreboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import net.kyori.adventure.text.Component;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** Drives the real Paper scoreboard API through MockBukkit to prove the sidebar renders and diffs natively. */
class SidebarManagerTest {

    private ServerMock server;
    private SidebarManager manager;

    private static Component c(String s) {
        return Component.text(s);
    }

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
    void createShowsASidebarObjectiveInTheSidebarSlot() {
        PlayerMock player = server.addPlayer();
        manager.create(player, c("Title"));

        Scoreboard board = player.getScoreboard();
        Objective objective = board.getObjective(DisplaySlot.SIDEBAR);
        assertThat(objective).isNotNull();
        assertThat(objective.displayName()).isEqualTo(c("Title"));
        assertThat(manager.count()).isEqualTo(1);
    }

    @Test
    void linesBecomeTeamPrefixesKeyedByInvisibleEntries() {
        PlayerMock player = server.addPlayer();
        Sidebar sidebar = manager.create(player, c("Title"));
        sidebar.lines(List.of(c("one"), c("two")));

        Scoreboard board = player.getScoreboard();
        Team first = board.getEntryTeam(Sidebar.entryKey(0));
        Team second = board.getEntryTeam(Sidebar.entryKey(1));
        assertThat(first).isNotNull();
        assertThat(first.prefix()).isEqualTo(c("one"));
        assertThat(second.prefix()).isEqualTo(c("two"));
    }

    @Test
    void editingOneLineLeavesTheOtherTeamUntouched() {
        PlayerMock player = server.addPlayer();
        Sidebar sidebar = manager.create(player, c("Title"));
        sidebar.lines(List.of(c("one"), c("two")));

        Scoreboard board = player.getScoreboard();
        Team unchangedBefore = board.getEntryTeam(Sidebar.entryKey(0));
        sidebar.lines(List.of(c("one"), c("CHANGED")));

        // Same Team object for line 0 (not re-created), and line 1's prefix updated in place.
        assertThat(board.getEntryTeam(Sidebar.entryKey(0))).isSameAs(unchangedBefore);
        assertThat(board.getEntryTeam(Sidebar.entryKey(1)).prefix()).isEqualTo(c("CHANGED"));
        assertThat(sidebar.currentLines()).containsExactly(c("one"), c("CHANGED"));
    }

    @Test
    void shrinkingRemovesTrailingTeamsAndScores() {
        PlayerMock player = server.addPlayer();
        Sidebar sidebar = manager.create(player, c("Title"));
        sidebar.lines(List.of(c("a"), c("b"), c("c")));
        sidebar.lines(List.of(c("a")));

        Scoreboard board = player.getScoreboard();
        assertThat(board.getEntryTeam(Sidebar.entryKey(1))).isNull();
        assertThat(board.getEntryTeam(Sidebar.entryKey(2))).isNull();
        assertThat(sidebar.currentLines()).containsExactly(c("a"));
    }

    @Test
    void createTwiceReplacesThePriorSidebar() {
        PlayerMock player = server.addPlayer();
        manager.create(player, c("First"));
        manager.create(player, c("Second"));

        Objective objective = player.getScoreboard().getObjective(DisplaySlot.SIDEBAR);
        assertThat(objective).isNotNull();
        assertThat(objective.displayName()).isEqualTo(c("Second"));
        assertThat(manager.count()).isEqualTo(1);
    }

    @Test
    void removeRestoresThePriorScoreboard() {
        PlayerMock player = server.addPlayer();
        Scoreboard before = player.getScoreboard();
        manager.create(player, c("Title"));
        assertThat(player.getScoreboard()).isNotSameAs(before);

        manager.remove(player);
        assertThat(player.getScoreboard()).isSameAs(before);
        assertThat(manager.count()).isZero();
    }

    @Test
    void forgetDropsWithoutRestoring() {
        PlayerMock player = server.addPlayer();
        manager.create(player, c("Title"));
        manager.forget(player.getUniqueId());
        assertThat(manager.count()).isZero();
        assertThat(manager.get(player.getUniqueId())).isNull();
    }

    @Test
    void boardSwitchCallbackFiresWithTheNewBoardOnCreate() {
        PlayerMock player = server.addPlayer();
        List<Switch> switches = new ArrayList<>();
        manager.onBoardSwitch((p, b) -> switches.add(new Switch(p, b)));

        manager.create(player, c("Title"));

        assertThat(switches).hasSize(1);
        assertThat(switches.get(0).player()).isSameAs(player);
        assertThat(switches.get(0).board()).isSameAs(player.getScoreboard());
    }

    @Test
    void boardSwitchCallbackFiresWithTheRestoredBoardOnRemove() {
        PlayerMock player = server.addPlayer();
        Scoreboard before = player.getScoreboard();
        manager.create(player, c("Title"));
        List<Switch> switches = new ArrayList<>();
        manager.onBoardSwitch((p, b) -> switches.add(new Switch(p, b)));

        manager.remove(player);

        assertThat(switches).hasSize(1);
        assertThat(switches.get(0).player()).isSameAs(player);
        assertThat(switches.get(0).board()).isSameAs(before);
        assertThat(player.getScoreboard()).isSameAs(before);
    }

    private record Switch(Player player, Scoreboard board) {}

    @Test
    void rejectsMoreLinesThanTheMaximum() {
        PlayerMock player = server.addPlayer();
        Sidebar sidebar = manager.create(player, c("Title"));
        List<Component> tooMany = java.util.stream.IntStream.range(0, Sidebar.MAX_LINES + 1)
                .mapToObj(i -> c("line" + i))
                .toList();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> sidebar.lines(tooMany))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
