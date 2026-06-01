package com.uxplima.uxmlib.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.kyori.adventure.text.Component;

import com.uxplima.uxmlib.gui.item.GuiItem;
import com.uxplima.uxmlib.gui.item.ItemPopulator;

/**
 * A menu that pages a long list of items through a fixed set of content slots. The full list is the
 * source of truth; each page render projects a window of it into the content slots, so items can be
 * added or cleared without recomputing slot math. Slots outside the content region are free for fixed
 * decorations and navigation buttons (wire those to {@link #nextPage()} / {@link #previousPage()}).
 *
 * <p>Created through {@link Guis#paginated()}.
 */
public final class PaginatedGui extends AbstractGui {

    private final List<GuiItem> pageItems = new ArrayList<>();
    private final List<Integer> contentSlots;
    private int page;

    PaginatedGui(Component title, int rows, List<Integer> contentSlots) {
        super(title, rows);
        Objects.requireNonNull(contentSlots, "contentSlots");
        if (contentSlots.isEmpty()) {
            throw new IllegalArgumentException("contentSlots must not be empty");
        }
        for (int slot : contentSlots) {
            if (slot < 0 || slot >= size()) {
                throw new IllegalArgumentException("content slot out of range: " + slot);
            }
        }
        this.contentSlots = List.copyOf(contentSlots);
    }

    /** Append an item to the paged list. Call {@link #open} or {@link #render} to show it. */
    public void addPageItem(GuiItem item) {
        Objects.requireNonNull(item, "item");
        pageItems.add(item);
    }

    /**
     * Replace the paged list by running every element of {@code source} through {@code populator}, which
     * maps each domain object to its icon and click action. The common case for a list-backed menu: give
     * it the data and a per-element renderer and the pages compute themselves.
     */
    public <T> void populate(java.util.List<T> source, ItemPopulator<T> populator) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(populator, "populator");
        pageItems.clear();
        page = 0;
        for (T element : source) {
            pageItems.add(populator.toItem(element));
        }
    }

    /** Remove every paged item (fixed decorations placed with {@code set} are kept). */
    public void clearPageItems() {
        pageItems.clear();
        page = 0;
    }

    /** The current page index, zero-based. */
    public int page() {
        return page;
    }

    /** The total number of pages (at least one). */
    public int pageCount() {
        int perPage = contentSlots.size();
        return Math.max(1, (pageItems.size() + perPage - 1) / perPage);
    }

    /** Advance one page if there is one, re-rendering the content slots. */
    public void nextPage() {
        if (page + 1 < pageCount()) {
            page++;
            render();
        }
    }

    /** Go back one page if there is one, re-rendering the content slots. */
    public void previousPage() {
        if (page > 0) {
            page--;
            render();
        }
    }

    /** Jump to {@code target} (clamped to a valid page) and open for {@code viewer}. */
    public void open(org.bukkit.entity.HumanEntity viewer, int target) {
        this.page = Math.max(0, Math.min(target, pageCount() - 1));
        open(viewer);
    }

    @Override
    public void open(org.bukkit.entity.HumanEntity viewer) {
        render();
        super.open(viewer);
    }

    /** Project the current page's window of items into the content slots. */
    public void render() {
        int perPage = contentSlots.size();
        int start = page * perPage;
        for (int i = 0; i < perPage; i++) {
            int slot = contentSlots.get(i);
            int index = start + i;
            if (index < pageItems.size()) {
                set(slot, pageItems.get(index));
            } else {
                remove(slot);
            }
        }
    }
}
