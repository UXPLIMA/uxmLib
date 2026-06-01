package com.uxplima.uxmlib.gui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/** Covers populating a paginated menu from a domain list via ItemPopulator. */
class ItemPopulatorTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private record Product(String name, int price) {}

    @Test
    void populatesPagesFromASourceList() {
        PaginatedGui gui = Guis.paginated().rows(2).build(); // 1 content row = 9 slots/page
        List<Product> products = List.of(
                new Product("a", 1),
                new Product("b", 2),
                new Product("c", 3),
                new Product("d", 4),
                new Product("e", 5),
                new Product("f", 6),
                new Product("g", 7),
                new Product("h", 8),
                new Product("i", 9),
                new Product("j", 10)); // 10 products -> 2 pages

        gui.populate(products, ItemPopulator.display(p -> new ItemStack(Material.PAPER, p.price())));

        assertThat(gui.pageCount()).isEqualTo(2);
        assertThat(gui.page()).isZero();
    }

    @Test
    void clickRoutesToTheBoundObject() {
        PaginatedGui gui = Guis.paginated().rows(2).build();
        String[] bought = {null};
        List<Product> products = List.of(new Product("sword", 100));

        gui.populate(
                products,
                ItemPopulator.of(p -> new ItemStack(Material.DIAMOND_SWORD), (p, event) -> bought[0] = p.name()));

        Player player = MockBukkit.getMock().addPlayer();
        gui.open(player);
        var view = java.util.Objects.requireNonNull(player.openInventory(gui.getInventory()));
        var event = new org.bukkit.event.inventory.InventoryClickEvent(
                view,
                org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,
                0, // first content slot holds the first product
                org.bukkit.event.inventory.ClickType.LEFT,
                org.bukkit.event.inventory.InventoryAction.PICKUP_ALL);

        gui.handleClick(event);

        assertThat(bought[0]).isEqualTo("sword");
    }

    @Test
    void populateReplacesPreviousContent() {
        PaginatedGui gui = Guis.paginated().rows(2).build();
        gui.populate(List.of(1, 2, 3), ItemPopulator.display(i -> new ItemStack(Material.STONE)));
        gui.nextPage();

        gui.populate(List.of(9), ItemPopulator.display(i -> new ItemStack(Material.DIRT)));

        assertThat(gui.pageCount()).isEqualTo(1);
        assertThat(gui.page()).isZero(); // reset on repopulate
    }
}
