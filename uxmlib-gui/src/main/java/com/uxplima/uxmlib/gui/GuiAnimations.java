package com.uxplima.uxmlib.gui;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import com.uxplima.uxmlib.gui.item.GuiItem;
import org.jspecify.annotations.Nullable;

/**
 * Owns a menu's animated content and its tick cadence: the attached {@link SlotAnimation} overlays, the
 * animation clock, and the {@code autoRefresh} interval that decides how often the changeable items are
 * re-rendered. Split out of {@code AbstractGui} so the per-tick painting lives in one place while the menu
 * class keeps the slot/inventory and click work.
 *
 * <p>This holder takes the inventory, item map, and owning menu as call parameters rather than keeping a
 * back-reference, so it has no view of menu state beyond what each tick is handed.
 */
final class GuiAnimations {

    private final List<SlotAnimation> animations = new ArrayList<>();
    private long ticks;
    private @Nullable Duration autoRefresh;
    private long refreshEveryTicks = 1L;

    /** The current animation-clock value, exposed so animated items can pick their frame. */
    long ticks() {
        return ticks;
    }

    /**
     * Attach {@code animation}. When the menu is already showing, its first frame is painted at once (rather
     * than waiting a tick to appear) by advancing it into {@code inv} over {@code items}.
     */
    void add(SlotAnimation animation, @Nullable Inventory inv, Map<Integer, GuiItem> items) {
        animations.add(Objects.requireNonNull(animation, "animation"));
        if (inv != null) {
            animation.advance(ticks, new InventorySink(inv, items));
        }
    }

    /** Whether any attached overlay or animated item means the menu must be re-rendered over time. */
    boolean hasAnimatedContent(Map<Integer, GuiItem> items) {
        return !animations.isEmpty() || items.values().stream().anyMatch(GuiItem.Animated.class::isInstance);
    }

    /** Whether this menu has animated or auto-refresh content and is currently being viewed. */
    boolean needsTicking(@Nullable Inventory inv, Map<Integer, GuiItem> items) {
        return (autoRefresh != null || hasAnimatedContent(items))
                && inv != null
                && !inv.getViewers().isEmpty();
    }

    /**
     * Advance the animation clock and, on the configured interval, re-render only the changeable items.
     * Static slots are left untouched, and an unchanged icon is not rewritten, so the tick path does the
     * least work it can.
     */
    void tick(@Nullable Inventory inv, Gui gui, Map<Integer, GuiItem> items) {
        ticks++;
        if (inv == null || ticks % refreshEveryTicks != 0) {
            return;
        }
        Player viewer = GuiRender.firstViewer(inv);
        if (viewer != null) {
            GuiRender.renderDynamic(inv, gui, items, viewer);
        }
        advance(inv, items);
    }

    /** Advance every attached overlay one step, painting only the slots its diff changed. */
    void advance(Inventory inv, Map<Integer, GuiItem> items) {
        if (animations.isEmpty()) {
            return;
        }
        InventorySink sink = new InventorySink(inv, items);
        for (SlotAnimation animation : animations) {
            animation.advance(ticks, sink);
        }
    }

    /** Set how often this menu re-renders while open ({@code null} = every tick, for animations). */
    void autoRefresh(@Nullable Duration interval) {
        this.autoRefresh = interval;
        this.refreshEveryTicks = interval == null ? 1L : Math.max(1L, interval.toMillis() / 50L);
    }
}
