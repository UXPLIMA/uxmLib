package com.uxplima.uxmlib.gui;

import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Pure, side-effect-free layout arithmetic shared by the paginated and typed menus. Kept as static helpers
 * so the size/clamp logic is unit-testable without a live inventory: deriving a sensible content page size
 * from a chest's row count, reporting a menu's true slot capacity (a non-chest {@link GuiType} is not nine
 * wide), and picking the nicest fixed-slot layout for a given item count.
 *
 * <p>Every method clamps to a usable value: a page size is never below one (so paging never divides by
 * zero), and a content area is never negative.
 */
public final class GuiLayout {

    private static final int WIDTH = 9;
    private static final int MAX_ROWS = 6;

    private GuiLayout() {}

    /**
     * The number of content rows for a chest of {@code rows} rows, reserving the bottom row for navigation
     * once the menu is tall enough to have one to spare. A single-row menu keeps its only row as content.
     */
    public static int contentRows(int rows) {
        if (rows < 1 || rows > MAX_ROWS) {
            throw new IllegalArgumentException("rows must be 1.." + MAX_ROWS);
        }
        return rows > 1 ? rows - 1 : rows;
    }

    /**
     * A sensible page size (content-slot count) for a chest of {@code rows} rows: the content rows times the
     * nine-wide grid, clamped to at least one so a caller can never page by zero.
     */
    public static int adaptivePageSize(int rows) {
        return Math.max(1, contentRows(rows) * WIDTH);
    }

    /** Clamp a caller-supplied content-slot {@code count} to at least one, so paging never divides by zero. */
    public static int clampPageSize(int count) {
        return Math.max(1, count);
    }

    /**
     * The true slot capacity of a {@code type} menu. A non-chest {@link GuiType} is not nine wide (a hopper
     * is five, a dispenser nine in a 3×3), so a filler must size off this rather than assuming {@code rows*9}.
     */
    public static int fillCapacity(GuiType type) {
        Objects.requireNonNull(type, "type");
        return type.size();
    }

    /**
     * Pick the nicest fixed slot layout for {@code count} items: the largest layout in {@code layouts} whose
     * key is at most {@code count}, falling back to the smallest layout when the count is below every key. So
     * one item can centre, three can make a tidy row, and so on, without per-count slot math at the call site.
     */
    public static List<Integer> adaptiveSlots(int count, NavigableMap<Integer, List<Integer>> layouts) {
        Objects.requireNonNull(layouts, "layouts");
        if (layouts.isEmpty()) {
            throw new IllegalArgumentException("layouts must not be empty");
        }
        var floor = layouts.floorEntry(count);
        return floor != null ? floor.getValue() : layouts.firstEntry().getValue();
    }

    /**
     * A convenience layout map mirroring nightcore's count→slots idiom: 1 item centres on a single-row menu,
     * up to three make a centred row, up to five fill the row. Use as the {@code layouts} for
     * {@link #adaptiveSlots} when a paginated menu wants to lay out small pages prettily.
     */
    public static NavigableMap<Integer, List<Integer>> singleRowLayouts() {
        NavigableMap<Integer, List<Integer>> layouts = new TreeMap<>();
        layouts.put(1, List.of(4));
        layouts.put(2, List.of(3, 5));
        layouts.put(3, List.of(3, 4, 5));
        layouts.put(4, List.of(2, 3, 5, 6));
        layouts.put(5, List.of(2, 3, 4, 5, 6));
        return layouts;
    }
}
