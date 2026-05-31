package com.uxplima.uxmlib.gui;

import org.bukkit.event.inventory.InventoryType;

/**
 * The non-chest menu shapes a {@link Gui} can take, each mapping to a Bukkit {@link InventoryType} with a
 * fixed slot count. Chest menus are sized by rows instead (see {@link Guis#gui()}); these cover the small
 * fixed-layout containers — hopper, dropper, dispenser, workbench, brewing stand.
 */
public enum GuiType {

    /** A 5-slot hopper. */
    HOPPER(InventoryType.HOPPER),

    /** A 9-slot (3×3) dispenser. */
    DISPENSER(InventoryType.DISPENSER),

    /** A 9-slot (3×3) dropper. */
    DROPPER(InventoryType.DROPPER),

    /** A crafting-table grid (result + 3×3). */
    WORKBENCH(InventoryType.WORKBENCH),

    /** A brewing stand. */
    BREWING(InventoryType.BREWING);

    private final InventoryType inventoryType;

    GuiType(InventoryType inventoryType) {
        this.inventoryType = inventoryType;
    }

    /** The backing Bukkit inventory type. */
    public InventoryType inventoryType() {
        return inventoryType;
    }

    /** The number of slots this menu has. */
    public int size() {
        return inventoryType.getDefaultSize();
    }
}
