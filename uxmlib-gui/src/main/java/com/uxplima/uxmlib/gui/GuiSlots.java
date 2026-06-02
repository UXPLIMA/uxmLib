package com.uxplima.uxmlib.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.bukkit.inventory.Inventory;

import com.uxplima.uxmlib.gui.item.GuiItem;
import org.jspecify.annotations.Nullable;

/**
 * Owns a menu's slot→item map and the slot-mutation API (set, add, remove, clear) over a fixed slot count.
 * Split out of {@code AbstractGui} so the slot bookkeeping and its bounds checks live in one place. Each
 * mutator that must touch the open inventory is handed the owning menu and its live inventory (or
 * {@code null} when it has not been built yet), so this holder keeps no view of inventory state of its own.
 *
 * <p>{@link #map()} returns the live backing map, the single source other collaborators (rendering, click
 * dispatch, animation) read and a subclass derives paged content from; the map is not copied on access.
 */
final class GuiSlots {

    private final int size;
    private final Map<Integer, GuiItem> items = new HashMap<>();

    GuiSlots(int size) {
        this.size = size;
    }

    /** The live slot→item map, the single source rendering, click dispatch, and subclasses read. */
    Map<Integer, GuiItem> map() {
        return items;
    }

    /** Put {@code item} at {@code slot}, writing it into {@code inv} immediately when the menu is showing. */
    void set(int slot, GuiItem item, Gui gui, @Nullable Inventory inv) {
        Objects.requireNonNull(item, "item");
        checkSlot(slot);
        items.put(slot, item);
        if (inv != null) {
            GuiRender.writeSlot(inv, gui, slot, item);
        }
    }

    /**
     * Put {@code item} at the 1-based {@code row}/{@code col} position, routed through {@code gui.set} so a
     * subclass that overrides single-slot placement (e.g. pagination's decoration tracking) still sees it.
     */
    void set(int row, int col, GuiItem item, Gui gui) {
        Objects.requireNonNull(item, "item");
        if (row < 1 || col < 1 || col > 9) {
            throw new IllegalArgumentException("row must be >= 1 and col must be 1..9");
        }
        gui.set((row - 1) * 9 + (col - 1), item);
    }

    /**
     * Fill the first empty slots with {@code newItems}; once the menu is full the rest are dropped. Each
     * placement is routed through {@code gui.set} so a subclass override still sees the added items.
     */
    void addItem(Gui gui, GuiItem... newItems) {
        Objects.requireNonNull(newItems, "items");
        int slot = 0;
        for (GuiItem item : newItems) {
            Objects.requireNonNull(item, "item");
            while (slot < size && items.containsKey(slot)) {
                slot++;
            }
            if (slot >= size) {
                return; // menu is full; the rest are silently dropped, matching addItem semantics
            }
            gui.set(slot, item);
            slot++;
        }
    }

    /** The item at {@code slot}, or {@code null} if the slot is empty. */
    @Nullable GuiItem get(int slot) {
        return items.get(slot);
    }

    /** Remove the item at {@code slot}, clearing it from {@code inv} when the menu is showing. */
    void remove(int slot, @Nullable Inventory inv) {
        checkSlot(slot);
        items.remove(slot);
        if (inv != null) {
            inv.clear(slot);
        }
    }

    /** Remove every item, clearing {@code inv} when the menu is showing. */
    void clear(@Nullable Inventory inv) {
        items.clear();
        if (inv != null) {
            inv.clear();
        }
    }

    private void checkSlot(int slot) {
        if (slot < 0 || slot >= size) {
            throw new IllegalArgumentException("slot must be 0.." + (size - 1));
        }
    }
}
