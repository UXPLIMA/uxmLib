package com.uxplima.uxmlib.item;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/**
 * Exercises the texture-extraction half of the live completer against a real (mock) Paper profile. The
 * {@code complete()} network round-trip cannot run under MockBukkit, so it is left to the resolver's
 * fake-completer tests; here we set a textures property directly and confirm {@link PaperProfileCompleter}
 * reads it back into a {@link SkullData.ByTexture}.
 */
class PaperProfileCompleterTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void extractsTheTexturesPropertyValue() {
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        profile.setProperty(new ProfileProperty("textures", "the-base64-texture-value"));

        Optional<SkullData.ByTexture> texture = PaperProfileCompleter.textureOf(profile);

        assertThat(texture).map(SkullData.ByTexture::base64).contains("the-base64-texture-value");
    }

    @Test
    void aProfileWithoutTexturesYieldsEmpty() {
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());

        assertThat(PaperProfileCompleter.textureOf(profile)).isEmpty();
    }
}
