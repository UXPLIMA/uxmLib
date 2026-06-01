package com.uxplima.uxmlib.common;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;

import org.jspecify.annotations.Nullable;

/**
 * Resolves a config string such as {@code "block.note_block.pling"} or {@code "minecraft:block.bell.use"}
 * to a native {@link Sound} via the sound registry. A bare key takes the {@code minecraft} namespace. Bad
 * input never throws: an unknown or malformed key yields {@link Optional#empty()}, so a typo in a config
 * cannot crash the caller.
 */
public final class Sounds {

    private Sounds() {}

    /**
     * Resolve {@code key} to a {@link Sound}, or empty if it is malformed or names no registered sound. The
     * key is lower-cased first, because {@link NamespacedKey} rejects the upper-case forms admins often type.
     */
    public static Optional<Sound> resolve(String key) {
        Objects.requireNonNull(key, "key");
        NamespacedKey parsed = NamespacedKey.fromString(key.trim().toLowerCase(Locale.ROOT));
        if (parsed == null) {
            return Optional.empty();
        }
        @Nullable Sound sound = Registry.SOUNDS.get(parsed);
        return Optional.ofNullable(sound);
    }
}
