package com.uxplima.uxmlib.gui.item;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.uxplima.uxmlib.gui.PaginatedGui;

/**
 * Maps a domain object of type {@code T} to the {@link GuiItem} that represents it in a
 * {@link PaginatedGui}. Build one with {@link #of} from an icon function and a click consumer that
 * receives both the clicked object and the event, so a list-backed menu binds its rows to data without
 * the caller writing the per-element wiring.
 */
@FunctionalInterface
public interface ItemPopulator<T> {

    /** The menu item for {@code element}. */
    GuiItem toItem(T element);

    /** Build a populator from an icon and a click handler that gets the clicked object and the event. */
    static <T> ItemPopulator<T> of(Function<T, ItemStack> icon, BiConsumer<T, InventoryClickEvent> onClick) {
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(onClick, "onClick");
        return element -> GuiItem.button(icon.apply(element), event -> onClick.accept(element, event));
    }

    /** Build a display-only populator (no click behaviour) from an icon function. */
    static <T> ItemPopulator<T> display(Function<T, ItemStack> icon) {
        Objects.requireNonNull(icon, "icon");
        return element -> GuiItem.display(icon.apply(element));
    }
}
