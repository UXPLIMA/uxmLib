package com.uxplima.uxmlib.gui;

import static org.assertj.core.api.Assertions.assertThat;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.uxplima.uxmlib.gui.item.GuiItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/** Covers attaching a {@link SlotAnimation} to a menu and advancing it on the menu's tick clock. */
class GuiAnimationWiringTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static ItemStack highlight() {
        return new ItemStack(Material.LIME_STAINED_GLASS_PANE);
    }

    @Test
    void animationIsConsideredTickingContent() {
        SimpleGui gui = Guis.gui().rows(3).build();
        assertThat(gui.hasAnimatedContent()).isFalse();
        gui.addAnimation(SlotAnimation.of(SlotPattern.clockwiseBorder(9, 3, 1, 0), highlight()));
        assertThat(gui.hasAnimatedContent()).isTrue();
    }

    @Test
    void tickWalksTheHighlightAroundTheBorder() {
        SimpleGui gui = Guis.gui().rows(3).build();
        gui.addAnimation(SlotAnimation.of(SlotPattern.clockwiseBorder(9, 3, 1, 0), highlight()));

        Player player = MockBukkit.getMock().addPlayer();
        gui.open(player); // applies frame 0 -> slot 0 lit

        assertThat(typeAt(gui, 0)).isEqualTo(Material.LIME_STAINED_GLASS_PANE);

        gui.tick(); // frame 1 -> slot 0 cleared, slot 1 lit
        assertThat(typeAt(gui, 0)).isEqualTo(Material.AIR);
        assertThat(typeAt(gui, 1)).isEqualTo(Material.LIME_STAINED_GLASS_PANE);
    }

    @Test
    void animationDoesNotClobberAPlacedButton() {
        SimpleGui gui = Guis.gui().rows(3).build();
        gui.set(0, GuiItem.display(new ItemStack(Material.DIAMOND))); // a button under the path
        gui.addAnimation(SlotAnimation.of(SlotPattern.clockwiseBorder(9, 3, 1, 0), highlight()));

        Player player = MockBukkit.getMock().addPlayer();
        gui.open(player); // frame 0 wants slot 0 but the diamond holds it

        assertThat(typeAt(gui, 0)).isEqualTo(Material.DIAMOND); // button survived

        gui.tick(); // frame 1 -> slot 1 lit, slot 0 untouched
        assertThat(typeAt(gui, 0)).isEqualTo(Material.DIAMOND);
        assertThat(typeAt(gui, 1)).isEqualTo(Material.LIME_STAINED_GLASS_PANE);
    }

    private static Material typeAt(SimpleGui gui, int slot) {
        ItemStack stack = gui.getInventory().getItem(slot);
        return stack == null ? Material.AIR : stack.getType();
    }
}
