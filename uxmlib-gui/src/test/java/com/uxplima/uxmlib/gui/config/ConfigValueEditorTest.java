package com.uxplima.uxmlib.gui.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import com.uxplima.uxmlib.config.HoconConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Covers the type-preserving parse/validate/set core behind the config editor (#20). */
class ConfigValueEditorTest {

    private static HoconConfig config(Path dir, String body) throws Exception {
        Path file = dir.resolve("config.conf");
        Files.writeString(file, body);
        return HoconConfig.load(file);
    }

    @Test
    void keepsAnIntegerKeyNumeric(@TempDir Path dir) throws Exception {
        HoconConfig config = config(dir, "max-homes = 3\n");
        ConfigValueEditor editor = new ConfigValueEditor(config);

        Object stored = editor.setAndSave("max-homes", "7");

        assertThat(stored).isEqualTo(7L);
        assertThat(config.getInt("max-homes", 0)).isEqualTo(7);
    }

    @Test
    void keepsABooleanKeyBoolean(@TempDir Path dir) throws Exception {
        HoconConfig config = config(dir, "enabled = true\n");
        ConfigValueEditor editor = new ConfigValueEditor(config);

        editor.setAndSave("enabled", "false");

        assertThat(config.getBoolean("enabled", true)).isFalse();
    }

    @Test
    void storesStringValuesVerbatim(@TempDir Path dir) throws Exception {
        HoconConfig config = config(dir, "title = \"Old\"\n");
        ConfigValueEditor editor = new ConfigValueEditor(config);

        editor.setAndSave("title", "New Title");

        assertThat(config.getString("title", "")).isEqualTo("New Title");
    }

    @Test
    void rejectsTextThatDoesNotFitANumericKey(@TempDir Path dir) throws Exception {
        HoconConfig config = config(dir, "max-homes = 3\n");
        ConfigValueEditor editor = new ConfigValueEditor(config);

        assertThatThrownBy(() -> editor.coerceAndSet("max-homes", "lots")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsTextThatIsNotABoolean(@TempDir Path dir) throws Exception {
        HoconConfig config = config(dir, "enabled = true\n");
        ConfigValueEditor editor = new ConfigValueEditor(config);

        assertThatThrownBy(() -> editor.coerceAndSet("enabled", "maybe")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void editsANestedScalarByDottedPath(@TempDir Path dir) throws Exception {
        HoconConfig config = config(dir, "economy { starting-balance = 100 }\n");
        ConfigValueEditor editor = new ConfigValueEditor(config);

        editor.setAndSave("economy.starting-balance", "250");

        assertThat(config.getInt("economy.starting-balance", 0)).isEqualTo(250);
    }
}
