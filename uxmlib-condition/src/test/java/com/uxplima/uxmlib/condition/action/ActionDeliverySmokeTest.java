package com.uxplima.uxmlib.condition.action;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Bukkit;

import com.uxplima.uxmlib.condition.OperandResolver;
import com.uxplima.uxmlib.text.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Smoke test for the native delivery wiring against a real (mock) Paper server: a {@code [message]} action
 * lands on a real player's Adventure audience, and a {@code [console]} action threads through {@code
 * Bukkit#dispatchCommand}. The parser and ordering logic are covered by their own pure tests; this asserts the
 * Adventure/Bukkit plumbing the closures rely on holds together end to end.
 */
class ActionDeliverySmokeTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void messageActionLandsOnTheRealPlayerAudience() {
        PlayerMock player = server.addPlayer("Steve");
        ActionContext context =
                ActionContext.builder(OperandResolver.identity()).player(player).build();

        ActionList.parse(List.of("[message] <green>hi there")).run(context);

        assertThat(Text.plain(player.nextComponentMessage())).isEqualTo("hi there");
    }

    @Test
    void consoleActionDispatchesThroughBukkit() {
        AtomicReference<String> dispatched = new AtomicReference<>();
        CommandSink consoleSink = command -> {
            dispatched.set(command);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        };
        ActionContext context = ActionContext.builder(OperandResolver.identity())
                .consoleSink(consoleSink)
                .build();

        ActionList.parse(List.of("[console] /say hello")).run(context);

        assertThat(dispatched.get()).isEqualTo("say hello");
    }
}
