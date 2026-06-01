package com.uxplima.uxmlib.hook;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/** Verifies the hook registry binds eagerly, defers until a plugin enables, and reports absent hooks. */
class HookRegistryTest {

    interface Weather {
        String today();
    }

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void aRegisteredHookIsRetrievableByType() {
        HookRegistry registry = new HookRegistry();
        registry.register(Weather.class, () -> "sunny");

        assertThat(registry.get(Weather.class)).isPresent();
        assertThat(registry.get(Weather.class).orElseThrow().today()).isEqualTo("sunny");
    }

    @Test
    void aDeferredHookBindsOnlyWhenItsPluginEnables() {
        HookRegistry registry = new HookRegistry();
        registry.defer(Weather.class, "SkyPlugin", () -> () -> "rainy");

        assertThat(registry.get(Weather.class)).isEmpty(); // not yet

        registry.onPluginEnabled("SkyPlugin");
        assertThat(registry.get(Weather.class)).isPresent();
        assertThat(registry.get(Weather.class).orElseThrow().today()).isEqualTo("rainy");
    }

    @Test
    void anUnknownTypeIsEmpty() {
        assertThat(new HookRegistry().get(Weather.class)).isEmpty();
    }

    @Test
    void bindPresentSweepsAlreadyEnabledPlugins() {
        HookRegistry registry = new HookRegistry();
        // No plugin named "Nope" is enabled, so bindPresent leaves it unbound.
        registry.defer(Weather.class, "Nope", () -> () -> "x");
        registry.bindPresent();
        assertThat(registry.get(Weather.class)).isEmpty();
    }
}
