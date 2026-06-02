package com.uxplima.uxmlib.item;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class UuidArrayTypeTest {

    @Test
    void roundTripsRandomUuidsThroughTheIntArray() {
        for (int i = 0; i < 1000; i++) {
            UUID id = UUID.randomUUID();
            int[] ints = UuidArrayType.toIntArray(id);
            assertThat(ints).hasSize(4);
            assertThat(UuidArrayType.fromIntArray(ints)).isEqualTo(id);
        }
    }

    @Test
    void encodesTheNilUuidAsFourZeroes() {
        UUID nil = new UUID(0L, 0L);
        assertThat(UuidArrayType.toIntArray(nil)).containsExactly(0, 0, 0, 0);
        assertThat(UuidArrayType.fromIntArray(new int[] {0, 0, 0, 0})).isEqualTo(nil);
    }

    @Test
    void matchesTheVanillaHighLowHalfSplit() {
        // Vanilla net.minecraft.core.UUIDUtil packs the two longs as {mostHi, mostLo, leastHi, leastLo}.
        UUID id = new UUID(0x0123456789ABCDEFL, 0xFEDCBA9876543210L);
        assertThat(UuidArrayType.toIntArray(id)).containsExactly(0x01234567, 0x89ABCDEF, 0xFEDCBA98, 0x76543210);
    }

    @Test
    void exposesIntArrayAsPrimitiveAndUuidAsComplex() {
        assertThat(UuidArrayType.INSTANCE.getPrimitiveType()).isEqualTo(int[].class);
        assertThat(UuidArrayType.INSTANCE.getComplexType()).isEqualTo(UUID.class);
    }
}
