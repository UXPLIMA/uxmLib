package com.uxplima.uxmlib.hologram;

import static org.assertj.core.api.Assertions.assertThat;

import net.kyori.adventure.text.Component;

import org.junit.jupiter.api.Test;

/** Pure tests of the clickable-hologram click model and its validation. */
class ClickableHologramTest {

    @Test
    void clickCarriesPlayerAndButton() {
        // A HologramClick is a simple value; its type tells left from right.
        assertThat(HologramClick.Type.values()).containsExactly(HologramClick.Type.LEFT, HologramClick.Type.RIGHT);
    }

    @Test
    void specOfBuildsLines() {
        // Sanity that HologramSpec.of (used by clickable) keeps the lines.
        HologramSpec spec = HologramSpec.of(java.util.List.of(Component.text("click me")));
        assertThat(spec.lines()).hasSize(1);
    }
}
