package com.uxplima.uxmlib.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

@org.jspecify.annotations.NullUnmarked
class LegacyCodeSanitizerTest {

    @Test
    void strips_ampersand_colour_code() {
        assertThat(LegacyCodeSanitizer.strip("&chello")).isEqualTo("hello");
    }

    @Test
    void strips_section_colour_code() {
        assertThat(LegacyCodeSanitizer.strip("§ahi")).isEqualTo("hi");
    }

    @Test
    void strips_format_codes() {
        assertThat(LegacyCodeSanitizer.strip("&kmagic&rplain")).isEqualTo("magicplain");
    }

    @Test
    void strips_hex_x_code() {
        assertThat(LegacyCodeSanitizer.strip("&xrainbow")).isEqualTo("rainbow");
    }

    @Test
    void keeps_ampersand_followed_by_space() {
        assertThat(LegacyCodeSanitizer.strip("Tom & Jerry")).isEqualTo("Tom & Jerry");
    }

    @Test
    void keeps_ampersand_followed_by_non_code_letter() {
        // 'z' is not a legacy code character.
        assertThat(LegacyCodeSanitizer.strip("R&Z")).isEqualTo("R&Z");
    }

    @Test
    void keeps_plain_text_unchanged() {
        assertThat(LegacyCodeSanitizer.strip("just words")).isEqualTo("just words");
    }

    @Test
    void mixed_text_strips_only_codes() {
        assertThat(LegacyCodeSanitizer.strip("&aHello &bworld & friends")).isEqualTo("Hello world & friends");
    }

    @Test
    void empty_returns_empty() {
        assertThat(LegacyCodeSanitizer.strip("")).isEmpty();
    }

    @Test
    void rejects_null() {
        assertThatNullPointerException().isThrownBy(() -> LegacyCodeSanitizer.strip(null));
    }
}
