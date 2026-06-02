package com.uxplima.uxmlib.gui;

import java.util.Objects;
import java.util.function.BiConsumer;

import org.bukkit.entity.HumanEntity;

import org.jspecify.annotations.Nullable;

/**
 * A menu bound to a domain object {@code T}, carried through render and re-render so a "delete home X?" or
 * "edit warp Y" menu always knows which {@code X} or {@code Y} it is acting on. The bound value is the source
 * of truth: change it with {@link #bind} and the menu re-renders against the new object, so one menu instance
 * serves a list of objects without rebuilding the GUI each time.
 *
 * <p>The renderer is a {@link BiConsumer} of the backing menu and the current value; it owns clearing and
 * laying out the slots for that object. A thin convenience over {@link SimpleGui}, pairing with
 * {@link GuiNavigator}.
 *
 * <pre>{@code
 * Linked<Home> menu = Linked.of(gui, (g, home) -> {
 *     g.set(13, GuiItem.button(home.icon(), e -> deleteHome(home)));
 * });
 * menu.bind(selectedHome).open(player);
 * }</pre>
 *
 * @param <T> the domain object the menu is bound to
 */
public final class Linked<T> {

    private final SimpleGui gui;
    private final BiConsumer<SimpleGui, T> renderer;
    private @Nullable T value;

    private Linked(SimpleGui gui, BiConsumer<SimpleGui, T> renderer) {
        this.gui = gui;
        this.renderer = renderer;
    }

    /** Bind {@code renderer} (called with the menu and the current value) over {@code gui}. */
    public static <T> Linked<T> of(SimpleGui gui, BiConsumer<SimpleGui, T> renderer) {
        Objects.requireNonNull(gui, "gui");
        Objects.requireNonNull(renderer, "renderer");
        return new Linked<>(gui, renderer);
    }

    /** Set the bound object and re-render the menu against it. Returns this for chaining with {@link #open}. */
    public Linked<T> bind(T value) {
        this.value = Objects.requireNonNull(value, "value");
        rerender();
        return this;
    }

    /** The currently bound object, or {@code null} if {@link #bind} has not been called yet. */
    public @Nullable T value() {
        return value;
    }

    /** Re-run the renderer against the current value (call after the bound object mutates in place). */
    public void rerender() {
        T current = value;
        if (current != null) {
            gui.clear();
            renderer.accept(gui, current);
        }
    }

    /** The backing menu. */
    public SimpleGui gui() {
        return gui;
    }

    /** Open the menu for {@code viewer}; binds nothing, so call {@link #bind} first. */
    public void open(HumanEntity viewer) {
        gui.open(Objects.requireNonNull(viewer, "viewer"));
    }
}
