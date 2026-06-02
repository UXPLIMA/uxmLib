package com.uxplima.uxmlib.item;

/**
 * An interaction that {@link ItemBlockerType} can forbid for a marked item. Each constant has a stable
 * {@link #id()} that is what gets written into the item's persistent data, so the enum may be reordered or
 * extended without invalidating already-stamped items. {@link ItemActionListener} maps each constant to the
 * Bukkit event it cancels.
 */
public enum ItemAction {

    /** Using the item as a crafting ingredient (the recipe result is suppressed). */
    CRAFT("craft"),

    /** Eating or drinking the item ({@code PlayerItemConsumeEvent}). */
    CONSUME("consume"),

    /** Placing the item as a block ({@code BlockPlaceEvent}). */
    PLACE("place"),

    /** Dropping the item on the ground ({@code PlayerDropItemEvent}). */
    DROP("drop");

    private final String id;

    ItemAction(String id) {
        this.id = id;
    }

    /** The stable identifier persisted for this action; never the enum {@code name()}. */
    public String id() {
        return id;
    }

    /** The action with this {@link #id()}, if any. */
    static java.util.Optional<ItemAction> byId(String id) {
        for (ItemAction action : values()) {
            if (action.id.equals(id)) {
                return java.util.Optional.of(action);
            }
        }
        return java.util.Optional.empty();
    }
}
