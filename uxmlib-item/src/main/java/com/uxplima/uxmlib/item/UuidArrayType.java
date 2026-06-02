package com.uxplima.uxmlib.item;

import java.util.Objects;
import java.util.UUID;

import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;

/**
 * Persists a {@link UUID} as the four-int array vanilla Minecraft itself uses, so a UUID written through this
 * type sits in NBT in the same {@code int[4]} shape as {@code net.minecraft.core.UUIDUtil} produces (and as
 * the vanilla {@code Owner}/entity UUID tags use). Storing UUIDs in the standard form keeps them readable by
 * other tools and avoids the bloat of a 36-char string. Register it like any built-in type:
 *
 * <pre>{@code pdc.set(key, UuidArrayType.INSTANCE, ownerId);}</pre>
 *
 * <p>The {@link #toIntArray(UUID)} / {@link #fromIntArray(int[])} split (most/least bits each cut into a high
 * and low int) follows the Item-NBT-API {@code UUIDUtil} (MIT), which mirrors the vanilla encoding.
 */
public final class UuidArrayType implements PersistentDataType<int[], UUID> {

    /** The single shared, stateless instance to pass to {@code PersistentDataContainer} reads and writes. */
    public static final UuidArrayType INSTANCE = new UuidArrayType();

    private UuidArrayType() {}

    /** Encode {@code id} as {@code {mostHi, mostLo, leastHi, leastLo}}, the vanilla {@code int[4]} layout. */
    public static int[] toIntArray(UUID id) {
        Objects.requireNonNull(id, "id");
        long most = id.getMostSignificantBits();
        long least = id.getLeastSignificantBits();
        return new int[] {(int) (most >> 32), (int) most, (int) (least >> 32), (int) least};
    }

    /** Decode a vanilla {@code int[4]} array back into the UUID it encodes. */
    public static UUID fromIntArray(int[] ints) {
        Objects.requireNonNull(ints, "ints");
        if (ints.length != 4) {
            throw new IllegalArgumentException("UUID int array must have length 4, was " + ints.length);
        }
        long most = (long) ints[0] << 32 | (ints[1] & 0xFFFFFFFFL);
        long least = (long) ints[2] << 32 | (ints[3] & 0xFFFFFFFFL);
        return new UUID(most, least);
    }

    @Override
    public Class<int[]> getPrimitiveType() {
        return int[].class;
    }

    @Override
    public Class<UUID> getComplexType() {
        return UUID.class;
    }

    @Override
    public int[] toPrimitive(UUID complex, PersistentDataAdapterContext context) {
        Objects.requireNonNull(complex, "complex");
        return toIntArray(complex);
    }

    @Override
    public UUID fromPrimitive(int[] primitive, PersistentDataAdapterContext context) {
        Objects.requireNonNull(primitive, "primitive");
        return fromIntArray(primitive);
    }
}
