package com.uxplima.uxmlib.text;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

@org.jspecify.annotations.NullUnmarked
class GlyphWidthTableTest {

    @Test
    void space_is_four_pixels() {
        assertThat(GlyphWidthTable.widthOf(' ', false)).isEqualTo(4);
    }

    @Test
    void narrow_glyphs_are_narrower_than_default() {
        assertThat(GlyphWidthTable.widthOf('i', false)).isEqualTo(2);
        assertThat(GlyphWidthTable.widthOf('l', false)).isEqualTo(3);
        assertThat(GlyphWidthTable.widthOf('!', false)).isEqualTo(2);
        assertThat(GlyphWidthTable.widthOf('.', false)).isEqualTo(2);
    }

    @Test
    void default_glyph_is_six_pixels() {
        assertThat(GlyphWidthTable.widthOf('A', false)).isEqualTo(6);
        assertThat(GlyphWidthTable.widthOf('z', false)).isEqualTo(6);
    }

    @Test
    void bold_adds_one_pixel() {
        assertThat(GlyphWidthTable.widthOf('A', true)).isEqualTo(7);
        assertThat(GlyphWidthTable.widthOf(' ', true)).isEqualTo(5);
    }

    @Test
    void unknown_code_point_falls_back_to_default() {
        assertThat(GlyphWidthTable.widthOf('☃', false)).isEqualTo(6); // snowman
    }
}
