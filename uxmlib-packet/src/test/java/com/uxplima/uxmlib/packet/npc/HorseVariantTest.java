package com.uxplima.uxmlib.packet.npc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Covers the pure horse-variant packing: a coat colour and a marking pack into the single integer the horse's
 * {@code DATA_ID_TYPE_VARIANT} metadata field carries, matching the server's own {@code setVariantAndMarkings}
 * ({@code color & 0xFF | markings << 8 & 0xFF00}). Kept here as a direct unit test because the packing is the
 * NMS-free, testable part of the horse appearance.
 */
class HorseVariantTest {

    @Test
    void packsColourIntoTheLowByteAndMarkingsIntoTheHighByte() {
        // White (0) with no markings (0) is zero; a colour alone lands in the low byte.
        assertThat(HorseVariant.pack(0, 0)).isEqualTo(0);
        assertThat(HorseVariant.pack(6, 0)).isEqualTo(6);
        // Markings alone shift into the high byte (markings << 8).
        assertThat(HorseVariant.pack(0, 4)).isEqualTo(4 << 8);
        // The two combine: colour 3, markings 2 -> 3 | (2 << 8) == 515.
        assertThat(HorseVariant.pack(3, 2)).isEqualTo(3 | (2 << 8));
        assertThat(HorseVariant.pack(3, 2)).isEqualTo(515);
    }

    @Test
    void masksEachComponentToItsByteSoAnOutOfRangeValueCannotBleed() {
        // The packing masks colour to the low byte and markings to the high byte, so an oversized value cannot
        // corrupt the other component (the plugin clamps to the real ranges; this is the defensive floor).
        assertThat(HorseVariant.pack(0x1FF, 0)).isEqualTo(0xFF);
        assertThat(HorseVariant.pack(0, 0x1FF)).isEqualTo(0xFF00);
    }
}
