package com.uxplima.uxmlib.packet.npc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * The pure rotation-to-byte encoding. These are the cardinal angles that must land on exact byte boundaries so
 * the standalone look/teleport packets stay in step with the spawn packet's own {@code Mth.packDegrees} packing.
 */
class ByteAngleTest {

    @Test
    void zeroDegreesIsZero() {
        assertThat(ByteAngle.of(0.0f)).isEqualTo((byte) 0);
    }

    @Test
    void ninetyDegreesIsAQuarterTurn() {
        assertThat(ByteAngle.of(90.0f)).isEqualTo((byte) 64);
    }

    @Test
    void oneHundredEightyDegreesWrapsToMinHalf() {
        assertThat(ByteAngle.of(180.0f)).isEqualTo((byte) -128);
    }

    @Test
    void twoHundredSeventyDegreesIsMinusAQuarter() {
        assertThat(ByteAngle.of(270.0f)).isEqualTo((byte) -64);
    }

    @Test
    void fullCircleWrapsBackToZero() {
        assertThat(ByteAngle.of(360.0f)).isEqualTo((byte) 0);
    }

    @Test
    void negativeAnglesMirrorTheirPositiveComplement() {
        assertThat(ByteAngle.of(-90.0f)).isEqualTo((byte) -64);
    }
}
