package com.uxplima.uxmlib.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Covers default-resource extraction, defaults auto-merge, and comment round-trip. */
class ConfigDefaultsTest {

    private static ClassLoader loader() {
        return ConfigDefaultsTest.class.getClassLoader();
    }

    @Test
    void extractsTheBundledDefaultOnFirstRun(@TempDir Path dir) throws Exception {
        Path target = dir.resolve("config.conf");
        assertThat(Files.exists(target)).isFalse();

        HoconConfig config = HoconConfig.loadOrExtract(target, "default-config.conf", loader());

        assertThat(Files.exists(target)).isTrue();
        assertThat(config.getInt("limit", 0)).isEqualTo(5);
        assertThat(config.getBoolean("feature.enabled", false)).isTrue();
        // The authored header comment survived the extraction (emitComments + byte-for-byte copy).
        assertThat(Files.readString(target)).contains("This header should survive extraction");
    }

    @Test
    void doesNotClobberAnExistingFile(@TempDir Path dir) throws Exception {
        Path target = dir.resolve("config.conf");
        Files.writeString(target, "limit = 99\n");

        HoconConfig config = HoconConfig.loadOrExtract(target, "default-config.conf", loader());

        assertThat(config.getInt("limit", 0)).isEqualTo(99); // user's value kept, default not applied
    }

    @Test
    void mergeDefaultsAddsMissingKeysButKeepsUserValues(@TempDir Path dir) throws Exception {
        Path target = dir.resolve("config.conf");
        Files.writeString(target, "limit = 99\n"); // user set limit, but not feature.enabled
        HoconConfig config = HoconConfig.load(target);

        boolean wrote = config.mergeDefaults("default-config.conf", loader());

        assertThat(wrote).isTrue();
        assertThat(config.getInt("limit", 0)).isEqualTo(99); // user value untouched
        assertThat(config.getBoolean("feature.enabled", false)).isTrue(); // missing key injected from default
    }

    @Test
    void mergeDefaultsWritesNothingWhenAlreadyComplete(@TempDir Path dir) throws Exception {
        Path target = dir.resolve("config.conf");
        HoconConfig config = HoconConfig.loadOrExtract(target, "default-config.conf", loader());

        boolean wrote = config.mergeDefaults("default-config.conf", loader());

        assertThat(wrote).isFalse(); // everything already present -> no rewrite
    }
}
