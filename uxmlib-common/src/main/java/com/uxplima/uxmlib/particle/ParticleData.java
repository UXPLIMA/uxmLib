package com.uxplima.uxmlib.particle;

import org.bukkit.Particle;

/**
 * Validation shared by the {@link ParticleOptions} records. A particle's {@link Particle#getDataType()}
 * declares the payload it expects; checking it at construction turns a "wrong data for this particle" mistake
 * into a clear exception at the call site instead of a {@link ClassCastException} deep inside the server.
 */
final class ParticleData {

    private ParticleData() {}

    /** Fail fast unless {@code particle} declares {@code expected} as its data type. */
    static void requireDataType(Particle particle, Class<?> expected) {
        Class<?> actual = particle.getDataType();
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException("particle " + particle + " expects data of type "
                    + actual.getSimpleName() + ", not " + expected.getSimpleName());
        }
    }

    /** Dust size is a render scale; a non-positive value would make the particle vanish. */
    static void requirePositiveSize(float size) {
        if (!(size > 0f)) {
            throw new IllegalArgumentException("size must be positive: " + size);
        }
    }
}
