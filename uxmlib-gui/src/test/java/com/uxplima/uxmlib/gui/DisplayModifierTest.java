package com.uxplima.uxmlib.gui;

import static org.assertj.core.api.Assertions.assertThat;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.uxplima.uxmlib.gui.item.DisplayModifier;
import com.uxplima.uxmlib.gui.item.DisplayModifiers;
import com.uxplima.uxmlib.gui.item.GuiItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/** Covers the per-viewer display-modifier pipeline applied during render. */
class DisplayModifierTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void modifierTransformsTheIconPerViewer() {
        SimpleGui gui = Guis.gui().rows(1).build();
        // A modifier that sets the icon's amount to the viewer's name length.
        DisplayModifier byNameLength = (ctx, base) ->
                new ItemStack(base.getType(), ctx.viewer().getName().length());
        GuiItem item = DisplayModifiers.apply(GuiItem.display(new ItemStack(Material.PAPER)), byNameLength);
        gui.set(0, item);

        Player steve = MockBukkit.getMock().addPlayer("Steve"); // length 5
        gui.open(steve);

        ItemStack rendered = gui.getInventory().getItem(0);
        assertThat(rendered).isNotNull();
        assertThat(rendered.getAmount()).isEqualTo(5);
    }

    @Test
    void pipelineAppliesModifiersInOrder() {
        SimpleGui gui = Guis.gui().rows(1).build();
        DisplayModifier plusOne = (ctx, base) -> new ItemStack(base.getType(), base.getAmount() + 1);
        DisplayModifier timesTwo = (ctx, base) -> new ItemStack(base.getType(), base.getAmount() * 2);
        GuiItem item = DisplayModifiers.apply(
                GuiItem.display(new ItemStack(Material.STONE, 1)), DisplayModifiers.of(plusOne, timesTwo));
        gui.set(0, item);

        Player player = MockBukkit.getMock().addPlayer();
        gui.open(player);

        ItemStack result = gui.getInventory().getItem(0);
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualTo(4); // (1+1)*2
    }

    @Test
    void placeholdersResolveAgainstTheEffectivePlayerNotTheViewer() {
        SimpleGui gui = Guis.gui().rows(1).build();
        Player admin = MockBukkit.getMock().addPlayer("Admin");
        Player target = MockBukkit.getMock().addPlayer("Target");
        // The resolver simply echoes the player's name, so the rendered name reveals which player was used.
        DisplayModifier echoName = DisplayModifiers.placeholders((player, text) -> player.getName());
        ItemStack icon = com.uxplima.uxmlib.item.ItemBuilder.of(Material.PAPER)
                .name(net.kyori.adventure.text.Component.text("ignored"))
                .build();
        GuiItem item = DisplayModifiers.apply(GuiItem.display(icon), echoName);

        var context = new com.uxplima.uxmlib.gui.item.RenderContext(admin, gui, 0).withEffectivePlayer(target);
        ItemStack rendered = item.icon(context);

        String name = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(
                        java.util.Objects.requireNonNull(rendered.getItemMeta()).displayName());
        assertThat(name).isEqualTo("Target");
    }

    @Test
    void loreSplitExpandsOneLineIntoSeveral() {
        SimpleGui gui = Guis.gui().rows(1).build();
        ItemStack icon = com.uxplima.uxmlib.item.ItemBuilder.of(Material.PAPER)
                .lore(
                        net.kyori.adventure.text.Component.text("first|second|third"),
                        net.kyori.adventure.text.Component.text("untouched"))
                .build();
        GuiItem item = DisplayModifiers.apply(GuiItem.display(icon), DisplayModifiers.loreSplit("|"));
        gui.set(0, item);

        Player player = MockBukkit.getMock().addPlayer();
        gui.open(player);

        ItemStack rendered = gui.getInventory().getItem(0);
        assertThat(rendered).isNotNull();
        var lore = java.util.Objects.requireNonNull(rendered.getItemMeta()).lore();
        assertThat(lore).hasSize(4); // three from the split line + the untouched one
    }

    @Test
    void keepsTheUnderlyingClickAction() {
        SimpleGui gui = Guis.gui().rows(1).build();
        boolean[] ran = {false};
        GuiItem button = GuiItem.button(new ItemStack(Material.STONE), e -> ran[0] = true);
        GuiItem modified = DisplayModifiers.apply(button, (ctx, base) -> base);
        gui.set(0, modified);

        Player player = MockBukkit.getMock().addPlayer();
        var view = java.util.Objects.requireNonNull(player.openInventory(gui.getInventory()));
        var event = new org.bukkit.event.inventory.InventoryClickEvent(
                view,
                org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,
                0,
                org.bukkit.event.inventory.ClickType.LEFT,
                org.bukkit.event.inventory.InventoryAction.PICKUP_ALL);

        gui.handleClick(event);

        assertThat(ran[0]).isTrue();
    }
}
