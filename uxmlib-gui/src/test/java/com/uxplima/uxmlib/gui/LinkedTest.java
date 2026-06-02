package com.uxplima.uxmlib.gui;

import static org.assertj.core.api.Assertions.assertThat;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.uxplima.uxmlib.gui.item.GuiItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/** Covers binding a menu to a domain object and re-rendering when that object changes. */
class LinkedTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private record Home(String name, Material icon) {}

    private static Linked<Home> homeMenu(SimpleGui gui) {
        return Linked.of(gui, (g, home) -> g.set(13, GuiItem.display(new ItemStack(home.icon()))));
    }

    @Test
    void bindRendersAgainstTheBoundObject() {
        SimpleGui gui = Guis.gui().rows(3).build();
        Linked<Home> menu = homeMenu(gui);

        menu.bind(new Home("base", Material.DIAMOND));

        GuiItem rendered = gui.getItem(13);
        assertThat(rendered).isInstanceOf(GuiItem.Static.class);
        assertThat(((GuiItem.Static) java.util.Objects.requireNonNull(rendered))
                        .item()
                        .getType())
                .isEqualTo(Material.DIAMOND);
        assertThat(menu.value()).isEqualTo(new Home("base", Material.DIAMOND));
    }

    @Test
    void rebindingSwapsTheRenderedContent() {
        SimpleGui gui = Guis.gui().rows(3).build();
        Linked<Home> menu = homeMenu(gui);

        menu.bind(new Home("base", Material.DIAMOND));
        menu.bind(new Home("shop", Material.EMERALD));

        assertThat(((GuiItem.Static) java.util.Objects.requireNonNull(gui.getItem(13)))
                        .item()
                        .getType())
                .isEqualTo(Material.EMERALD);
    }

    @Test
    void rerenderClearsStaleSlotsFromThePreviousObject() {
        SimpleGui gui = Guis.gui().rows(3).build();
        // A renderer that places its item in a slot keyed off the object, so a re-render must clear the old.
        Linked<Home> menu = Linked.of(gui, (g, home) -> {
            int slot = home.name().equals("base") ? 0 : 8;
            g.set(slot, GuiItem.display(new ItemStack(home.icon())));
        });

        menu.bind(new Home("base", Material.DIAMOND));
        assertThat(gui.getItem(0)).isNotNull();

        menu.bind(new Home("shop", Material.EMERALD));
        assertThat(gui.getItem(0)).isNull(); // cleared on re-render
        assertThat(gui.getItem(8)).isNotNull();
    }

    @Test
    void valueIsNullBeforeBinding() {
        Linked<Home> menu = homeMenu(Guis.gui().rows(1).build());
        assertThat(menu.value()).isNull();
    }
}
