package com.uxplima.uxmlib.gui;

/**
 * A single class of inventory interaction a {@link Gui} can choose to allow. By default a menu cancels
 * every interaction, so its items cannot be taken, replaced, or dragged out. Allowing a modifier lets
 * that one class of interaction through — for instance a storage menu allows {@link #ITEM_TAKE} and
 * {@link #ITEM_PLACE} so a player can move real items in and out, while a button menu allows none.
 */
public enum InteractionModifier {

    /** Taking an item out of a menu slot. */
    ITEM_TAKE,

    /** Putting an item into a menu slot. */
    ITEM_PLACE,

    /** Swapping the cursor item with a menu slot's item. */
    ITEM_SWAP,

    /** Dropping an item (Q / drop key) while the menu is open. */
    ITEM_DROP;

    /** The modifier a click {@code action} falls under, or {@code null} if it is not a take/place/swap/drop. */
    static @org.jspecify.annotations.Nullable InteractionModifier forAction(
            org.bukkit.event.inventory.InventoryAction action) {
        return switch (action) {
            case PICKUP_ALL,
                    PICKUP_HALF,
                    PICKUP_SOME,
                    PICKUP_ONE,
                    MOVE_TO_OTHER_INVENTORY,
                    COLLECT_TO_CURSOR -> ITEM_TAKE;
            case PLACE_ALL, PLACE_SOME, PLACE_ONE -> ITEM_PLACE;
            case SWAP_WITH_CURSOR, HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> ITEM_SWAP;
            case DROP_ALL_SLOT, DROP_ONE_SLOT, DROP_ALL_CURSOR, DROP_ONE_CURSOR -> ITEM_DROP;
            default -> null;
        };
    }
}
