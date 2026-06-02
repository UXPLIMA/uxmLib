package com.uxplima.uxmlib.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.uxplima.uxmlib.gui.config.MenuConditions;
import com.uxplima.uxmlib.gui.item.GuiAction;
import com.uxplima.uxmlib.gui.item.GuiItem;
import com.uxplima.uxmlib.gui.item.RenderContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/** Covers the named-condition registry and first-matching-state selection that drives a config menu. */
class MenuConditionsTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void registryReturnsRegisteredConditionsAndAlwaysIsBuiltIn() {
        MenuConditions conditions = new MenuConditions();
        conditions.register("named-alex", ctx -> ctx.viewer().getName().startsWith("A"));

        assertThat(conditions.get("always")).isNotNull();
        assertThat(conditions.get("named-alex")).isNotNull();
        assertThat(conditions.get("missing")).isNull();
    }

    @Test
    void requireThrowsOnAnUnknownConditionName() {
        MenuConditions conditions = new MenuConditions();
        assertThatThrownBy(() -> conditions.require("nope")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void firstMatchingConditionSelectsItsIcon() {
        MenuConditions conditions = new MenuConditions();
        conditions.register("starts-a", ctx -> ctx.viewer().getName().startsWith("A"));

        GuiItem.Stateful item = MenuConditions.statefulOf(java.util.List.of(
                new MenuConditions.NamedState(
                        "vip",
                        conditions.require("starts-a"),
                        new ItemStack(Material.EMERALD),
                        GuiAction.None.INSTANCE),
                new MenuConditions.NamedState(
                        "default",
                        conditions.require("always"),
                        new ItemStack(Material.REDSTONE),
                        GuiAction.None.INSTANCE)));

        SimpleGui gui = Guis.gui().rows(1).build();
        gui.set(0, item);

        Player alex = MockBukkit.getMock().addPlayer("Alex");
        gui.open(alex);
        assertThat(java.util.Objects.requireNonNull(gui.getInventory().getItem(0))
                        .getType())
                .isEqualTo(Material.EMERALD);

        Player steve = MockBukkit.getMock().addPlayer("Steve");
        gui.open(steve);
        assertThat(java.util.Objects.requireNonNull(gui.getInventory().getItem(0))
                        .getType())
                .isEqualTo(Material.REDSTONE);
    }

    @Test
    void parseResolvesABareNameToTheRegisteredCondition() {
        MenuConditions conditions = new MenuConditions();
        conditions.register("starts-a", ctx -> ctx.viewer().getName().startsWith("A"));

        Player alex = MockBukkit.getMock().addPlayer("Alex");
        Player steve = MockBukkit.getMock().addPlayer("Steve");
        SimpleGui gui = Guis.gui().rows(1).build();

        var predicate = conditions.parse("starts-a");
        assertThat(predicate.test(new RenderContext(alex, gui, 0))).isTrue();
        assertThat(predicate.test(new RenderContext(steve, gui, 0))).isFalse();
    }

    @Test
    void parseNegatesWithALeadingBang() {
        MenuConditions conditions = new MenuConditions();
        conditions.register("starts-a", ctx -> ctx.viewer().getName().startsWith("A"));

        Player alex = MockBukkit.getMock().addPlayer("Alex");
        SimpleGui gui = Guis.gui().rows(1).build();

        assertThat(conditions.parse("!starts-a").test(new RenderContext(alex, gui, 0)))
                .isFalse();
    }

    @Test
    void parseCombinesWithAndOr() {
        MenuConditions conditions = new MenuConditions();
        conditions.register("starts-a", ctx -> ctx.viewer().getName().startsWith("A"));
        conditions.register("len-4", ctx -> ctx.viewer().getName().length() == 4);

        Player alex = MockBukkit.getMock().addPlayer("Alex"); // starts A, length 4
        Player steve = MockBukkit.getMock().addPlayer("Steve"); // neither
        SimpleGui gui = Guis.gui().rows(1).build();

        assertThat(conditions.parse("starts-a && len-4").test(new RenderContext(alex, gui, 0)))
                .isTrue();
        assertThat(conditions.parse("starts-a && len-4").test(new RenderContext(steve, gui, 0)))
                .isFalse();
        assertThat(conditions.parse("starts-a || len-4").test(new RenderContext(alex, gui, 0)))
                .isTrue();
        assertThat(conditions.parse("starts-a || len-4").test(new RenderContext(steve, gui, 0)))
                .isFalse();
    }

    @Test
    void parseRejectsAnUnknownTermAndBlankExpression() {
        MenuConditions conditions = new MenuConditions();
        assertThatThrownBy(() -> conditions.parse("nope")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> conditions.parse("   ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noMatchingStateRendersEmpty() {
        GuiItem.Stateful item = MenuConditions.statefulOf(java.util.List.of(new MenuConditions.NamedState(
                "never", ctx -> false, new ItemStack(Material.DIAMOND), GuiAction.None.INSTANCE)));

        SimpleGui gui = Guis.gui().rows(1).build();
        gui.set(0, item);
        Player player = MockBukkit.getMock().addPlayer();
        gui.open(player);

        ItemStack rendered = gui.getInventory().getItem(0);
        assertThat(rendered == null || rendered.getType() == Material.AIR).isTrue();
    }
}
