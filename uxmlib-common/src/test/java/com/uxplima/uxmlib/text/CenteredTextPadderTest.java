package com.uxplima.uxmlib.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

@org.jspecify.annotations.NullUnmarked
class CenteredTextPadderTest {

    @Test
    void short_line_gets_leading_spaces() {
        String pad = CenteredTextPadder.pad("Hi", false);
        // "Hi" = H(6) + i(2) = 8px; (320 - 8) / 2 = 156px; 156 / 4 = 39 spaces.
        assertThat(pad).isEqualTo(" ".repeat(39));
    }

    @Test
    void empty_string_centres_full_width() {
        // 0px text → (320 - 0) / 2 = 160px → 40 spaces.
        assertThat(CenteredTextPadder.pad("", false)).isEqualTo(" ".repeat(40));
    }

    @Test
    void over_wide_line_gets_no_padding() {
        String wide = "W".repeat(100); // 600px >> 320
        assertThat(CenteredTextPadder.pad(wide, false)).isEmpty();
    }

    @Test
    void bold_text_is_wider_so_fewer_pad_spaces() {
        String plain = CenteredTextPadder.pad("Hi", false);
        String bold = CenteredTextPadder.pad("Hi", true);
        assertThat(bold.length()).isLessThan(plain.length());
    }

    @Test
    void rejects_null() {
        assertThatNullPointerException().isThrownBy(() -> CenteredTextPadder.pad(null, false));
    }
}
