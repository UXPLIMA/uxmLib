package com.uxplima.uxmlib.gui;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.bukkit.inventory.ItemStack;

import org.jspecify.annotations.Nullable;

/**
 * A moving-highlight overlay: a {@link SlotPattern} of lit slots plus the icon shown in them, advanced on a
 * tick clock. Each advance diffs the new frame against the last one and writes only the slots that changed
 * (clear the slots it lit last time that are now dark; light the slots in the new frame that are free).
 *
 * <p>The clear is guarded: the overlay only ever clears slots it lit itself, and only lights slots the
 * target reports free, so it never clobbers a button a caller placed under the path of the animation. The
 * actual inventory writes go through a {@link Sink}, so the frame/diff bookkeeping is testable without a
 * live inventory and the menu supplies the real write.
 */
public final class SlotAnimation {

    private final SlotPattern pattern;
    private final ItemStack icon;
    private @Nullable Integer lastFrame;
    private final Set<Integer> ownedSlots = new HashSet<>();

    private SlotAnimation(SlotPattern pattern, ItemStack icon) {
        this.pattern = Objects.requireNonNull(pattern, "pattern");
        this.icon = Objects.requireNonNull(icon, "icon").clone();
    }

    /** Bind {@code pattern} to the highlight {@code icon}; the icon is cloned so later mutation is harmless. */
    public static SlotAnimation of(SlotPattern pattern, ItemStack icon) {
        return new SlotAnimation(pattern, icon);
    }

    /** The pattern this overlay animates. */
    public SlotPattern pattern() {
        return pattern;
    }

    /**
     * Advance to the frame for {@code ticks} and apply only the changed slots through {@code sink}. A no-op
     * when the frame has not changed since the last advance, so a menu that re-ticks at the same frame does
     * no inventory work.
     */
    public void advance(long ticks, Sink sink) {
        Objects.requireNonNull(sink, "sink");
        int frame = pattern.frameIndexAt(ticks);
        if (lastFrame != null && lastFrame == frame) {
            return;
        }
        clearOwnedNotIn(pattern.frame(frame), sink);
        lightFree(pattern.frame(frame), sink);
        lastFrame = frame;
    }

    private void clearOwnedNotIn(List<Integer> nextFrame, Sink sink) {
        Set<Integer> keep = new HashSet<>(nextFrame);
        for (Integer slot : new HashSet<>(ownedSlots)) {
            if (!keep.contains(slot)) {
                sink.clear(slot);
                ownedSlots.remove(slot);
            }
        }
    }

    private void lightFree(List<Integer> nextFrame, Sink sink) {
        for (int slot : nextFrame) {
            if (ownedSlots.contains(slot) || sink.isFree(slot)) {
                sink.light(slot, icon);
                ownedSlots.add(slot);
            }
        }
    }

    /**
     * The inventory writes an animation needs, abstracted so the diff is testable. {@link #isFree} guards
     * the lighting (do not paint over a button), {@link #light} writes the highlight, {@link #clear} removes
     * it. The animation only ever calls {@link #clear} on slots it previously lit.
     */
    public interface Sink {

        /** Whether {@code slot} is empty and may be painted (a guard against clobbering a placed item). */
        boolean isFree(int slot);

        /** Show the highlight {@code icon} in {@code slot}. */
        void light(int slot, ItemStack icon);

        /** Remove the highlight from {@code slot} (called only for slots this animation lit). */
        void clear(int slot);
    }
}
