package com.uxplima.uxmlib.condition;

import static org.assertj.core.api.Assertions.assertThat;

import org.bukkit.entity.Player;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Smoke test for the Player-bound wiring against a real (mock) Paper server: a request built for a player
 * threads that player into the resolver, and a placeholder condition keyed off it evaluates end to end. The
 * pure comparison and list logic are covered by their own unit tests; this asserts the subject plumbing
 * (player into resolver, resolver into comparison) holds together.
 */
class PlayerConditionSmokeTest {

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
    void requestThreadsThePlayerIntoTheResolver() {
        PlayerMock player = server.addPlayer("Steve");
        // A resolver that answers a name placeholder from the actual subject player passed by the request.
        OperandResolver resolver = (p, template) -> {
            if ("%name%".equals(template) && p != null) {
                return p.getName();
            }
            return template;
        };
        PlaceholderCondition condition = PlaceholderCondition.of("%name%", Operator.EQUAL, "Steve");

        ConditionRequest request =
                ConditionRequest.builder(resolver).player(player).build();

        assertThat(request.player()).contains((Player) player);
        assertThat(condition.test(request)).isTrue();
    }

    @Test
    void forPlayerFactoryCarriesTheSubject() {
        PlayerMock player = server.addPlayer();
        ConditionRequest request = ConditionRequest.forPlayer(player);
        assertThat(request.player()).contains((Player) player);
        assertThat(request.isCancelled()).isFalse();
    }
}
