package com.uxplima.uxmlib.text;

import java.util.Map;

/**
 * Minecraft default-font advance widths, in pixels (including the trailing 1px gap between glyphs). Used by
 * {@link CenteredTextPadder} to compute the leading-space padding that centres a line within the chat width.
 *
 * <p>Most glyphs are 6px; a small set of narrow glyphs ({@code i}, {@code l}, {@code !}, {@code .},
 * {@code :}, {@code ,}, …) are narrower, and the space is 4px. Bold text adds 1px per glyph. Unknown code
 * points fall back to the 6px default rather than throwing — centring is cosmetic, so a ±1px error on an
 * exotic glyph is invisible.
 */
public final class GlyphWidthTable {

    private GlyphWidthTable() {}

    /** Default advance for any glyph not in the narrow set. */
    public static final int DEFAULT_WIDTH = 6;

    /** Advance for the space character. */
    public static final int SPACE_WIDTH = 4;

    /** Extra pixels a bold glyph adds. */
    public static final int BOLD_EXTRA = 1;

    /** Narrow glyphs (default-font advances, trailing-gap-inclusive). */
    private static final Map<Character, Integer> NARROW = Map.ofEntries(
            Map.entry(' ', SPACE_WIDTH),
            Map.entry('i', 2),
            Map.entry('l', 3),
            Map.entry('!', 2),
            Map.entry('.', 2),
            Map.entry(',', 2),
            Map.entry(':', 2),
            Map.entry(';', 2),
            Map.entry('\'', 2),
            Map.entry('|', 2),
            Map.entry('I', 4),
            Map.entry('t', 4),
            Map.entry('f', 5),
            Map.entry('k', 5),
            Map.entry('(', 5),
            Map.entry(')', 5),
            Map.entry('{', 5),
            Map.entry('}', 5),
            Map.entry('[', 4),
            Map.entry(']', 4),
            Map.entry('<', 5),
            Map.entry('>', 5));

    /** Pixel advance of {@code c}, accounting for bold. */
    public static int widthOf(char c, boolean bold) {
        int base = NARROW.getOrDefault(c, DEFAULT_WIDTH);
        return bold ? base + BOLD_EXTRA : base;
    }
}
