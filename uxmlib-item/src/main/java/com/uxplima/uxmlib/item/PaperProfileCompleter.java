package com.uxplima.uxmlib.item;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

/**
 * The default {@link ProfileCompleter}: it builds a Paper {@link PlayerProfile} from the key (a UUID string
 * if it parses as one, otherwise a name), fills it in with the blocking {@link PlayerProfile#complete()}, and
 * reads back the {@code textures} property as a {@link SkullData.ByTexture}. A profile with no textures
 * property resolves to an empty {@link Optional} so {@link SkullResolver} can cache the miss.
 *
 * <p>{@code complete()} performs a network round-trip to Mojang, so this must only ever run on the scheduler's
 * async pool. {@link SkullResolver} guarantees that; do not call it directly from a server thread.
 */
public final class PaperProfileCompleter implements ProfileCompleter {

    @Override
    public Optional<SkullData.ByTexture> complete(String key) {
        Objects.requireNonNull(key, "key");
        PlayerProfile profile = createProfile(key);
        profile.complete();
        return textureOf(profile);
    }

    // Pull the textures property out of a (completed) profile. Split out so the pure extraction is testable
    // without the network round-trip that complete() performs.
    static Optional<SkullData.ByTexture> textureOf(PlayerProfile profile) {
        return profile.getProperties().stream()
                .filter(property -> "textures".equals(property.getName()))
                .map(ProfileProperty::getValue)
                .filter(value -> !value.isBlank())
                .map(SkullData.ByTexture::new)
                .findFirst();
    }

    private static PlayerProfile createProfile(String key) {
        Optional<UUID> uuid = parseUuid(key);
        return uuid.map(Bukkit::createProfile).orElseGet(() -> Bukkit.createProfile(key));
    }

    private static Optional<UUID> parseUuid(String key) {
        try {
            return Optional.of(UUID.fromString(key));
        } catch (IllegalArgumentException notAUuid) {
            return Optional.empty();
        }
    }
}
