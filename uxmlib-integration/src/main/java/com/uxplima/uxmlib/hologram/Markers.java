package com.uxplima.uxmlib.hologram;

import java.util.Objects;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

/**
 * Stamps every hologram-related entity (text/item/block displays and interaction boxes) with a persistent
 * marker so it can be told apart from any other entity in the world. Because these entities persist, this
 * lets {@link HologramManager} sweep up holograms orphaned by a crash on the next startup. The key is
 * created once, never on a hot path.
 */
final class Markers {

    private static final NamespacedKey KEY =
            Objects.requireNonNull(NamespacedKey.fromString("uxmlib:hologram"), "marker key");

    private Markers() {}

    /** Tag {@code entity} as part of a uxmLib hologram. */
    static void stamp(Entity entity) {
        entity.getPersistentDataContainer().set(KEY, PersistentDataType.BOOLEAN, true);
    }

    /** Whether {@code entity} carries the uxmLib hologram marker. */
    static boolean isHologram(Entity entity) {
        return entity.getPersistentDataContainer().has(KEY, PersistentDataType.BOOLEAN);
    }
}
