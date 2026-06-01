package com.uxplima.uxmlib.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.bukkit.entity.HumanEntity;

import net.kyori.adventure.text.Component;

import com.uxplima.uxmlib.gui.item.GuiItem;

/**
 * A chest menu that scrolls a long list of items past a fixed window, one row (vertical) or one column
 * (horizontal) at a time, rather than jumping a whole page like {@link PaginatedGui}. Items are laid out
 * into the grid in reading order and the window slides over them: vertical scrolling reveals later rows,
 * horizontal scrolling reveals later columns. Wire {@link #scrollNext()} / {@link #scrollPrevious()} to
 * buttons placed in slots the layout leaves free.
 *
 * <p>Created through {@link Guis#scrolling(ScrollType)}.
 */
public final class ScrollingGui extends AbstractGui {

    private static final int WIDTH = 9;

    private final ScrollType scrollType;
    private final int rows;
    private final List<GuiItem> scrollItems = new ArrayList<>();
    private int offset; // the first row (vertical) or column (horizontal) of the visible window

    ScrollingGui(Component title, int rows, ScrollType scrollType) {
        super(title, rows);
        this.rows = rows;
        this.scrollType = Objects.requireNonNull(scrollType, "scrollType");
    }

    /** Append an item to the scrollable list. */
    public void addScrollItem(GuiItem item) {
        scrollItems.add(Objects.requireNonNull(item, "item"));
    }

    /** Remove every scrollable item and reset to the start. */
    public void clearScrollItems() {
        scrollItems.clear();
        offset = 0;
    }

    /** The current scroll offset (rows scrolled for vertical, columns for horizontal), zero-based. */
    public int offset() {
        return offset;
    }

    /** The greatest offset that still shows new content; scrolling past it is a no-op. */
    public int maxOffset() {
        int lines = scrollType == ScrollType.VERTICAL ? rows : WIDTH;
        int span = scrollType == ScrollType.VERTICAL ? WIDTH : rows;
        int totalLines = (scrollItems.size() + span - 1) / span;
        return Math.max(0, totalLines - lines);
    }

    /** Scroll forward one row/column if there is more to show, re-rendering. */
    public void scrollNext() {
        if (offset < maxOffset()) {
            offset++;
            render();
        }
    }

    /** Scroll back one row/column if not already at the start, re-rendering. */
    public void scrollPrevious() {
        if (offset > 0) {
            offset--;
            render();
        }
    }

    @Override
    public void open(HumanEntity viewer) {
        render();
        super.open(viewer);
    }

    /** Project the visible window of the scroll list into the grid. */
    public void render() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < WIDTH; col++) {
                int slot = row * WIDTH + col;
                int index = indexFor(row, col);
                if (index >= 0 && index < scrollItems.size()) {
                    set(slot, scrollItems.get(index));
                } else {
                    remove(slot);
                }
            }
        }
    }

    private int indexFor(int row, int col) {
        // Vertical: items fill left-to-right then top-to-bottom; the window starts `offset` rows down.
        // Horizontal: items fill top-to-bottom then left-to-right; the window starts `offset` columns over.
        if (scrollType == ScrollType.VERTICAL) {
            return (row + offset) * WIDTH + col;
        }
        return (col + offset) * rows + row;
    }
}
