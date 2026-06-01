package com.uxplima.uxmlib.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Covers schema-versioned config migration: rename/remove keys, replay only newer steps. */
class ConfigMigrationTest {

    private static ConfigMigration twoStepMigration() {
        return ConfigMigration.builder()
                .version(1, step -> step.rename("oldName", "name"))
                .version(2, step -> step.remove("legacy"))
                .build();
    }

    @Test
    void upgradesAnOldFileRenamingAndRemovingKeys(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.conf");
        Files.writeString(file, "oldName = server\nlegacy = junk\n"); // no config-version -> version unknown
        HoconConfig config = HoconConfig.load(file);

        int now = config.migrate(twoStepMigration());

        assertThat(now).isEqualTo(2);
        assertThat(config.getString("name", "")).isEqualTo("server"); // renamed
        assertThat(config.getString("oldName")).isEmpty(); // old key gone
        assertThat(config.getString("legacy")).isEmpty(); // removed
    }

    @Test
    void replaysOnlyNewerStepsAndRecordsVersion(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.conf");
        // Already at version 1, so the rename step is skipped; only step 2 (remove legacy) runs.
        Files.writeString(file, "config-version = 1\nname = server\nlegacy = junk\noldName = stale\n");
        HoconConfig config = HoconConfig.load(file);

        int now = config.migrate(twoStepMigration());

        assertThat(now).isEqualTo(2);
        assertThat(config.getString("legacy")).isEmpty(); // step 2 ran
        assertThat(config.getString("oldName", "")).isEqualTo("stale"); // step 1 skipped, old key untouched
    }

    @Test
    void aCurrentFileMigratesToNoChange(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.conf");
        Files.writeString(file, "config-version = 2\nname = server\n");
        HoconConfig config = HoconConfig.load(file);

        int now = config.migrate(twoStepMigration());
        assertThat(now).isEqualTo(2);
        assertThat(config.getString("name", "")).isEqualTo("server");
    }
}
