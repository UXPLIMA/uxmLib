package com.uxplima.uxmlib.gui;

import java.util.Objects;
import java.util.function.Consumer;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

import org.jspecify.annotations.Nullable;

/**
 * Holds a menu's user-supplied open/close/default-click/outside-click handlers together with the
 * close-control state that surrounds them: the title-change reopen guard, the prevent-close flag, and the
 * programmatic-close flag. Split out of {@code AbstractGui} so the handler fields and the guards that
 * decide when those handlers fire live in one place, leaving the menu class to the slot/inventory work.
 *
 * <p>The flags read like a small state machine, so they are kept together:
 *
 * <ul>
 *   <li>{@code reopening} is set only while {@code updateTitle} rebuilds and reopens the inventory, so the
 *       internal close/reopen does not look like a real close or open to the user's handlers.
 *   <li>{@code preventClose} makes a client-driven close (Escape) reopen the menu next tick, so a
 *       forced-input menu cannot be dismissed by the viewer.
 *   <li>{@code closingProgrammatically} is set for the span of an API-driven {@code close()} /
 *       {@code closeAll()} so a deliberate close still closes a prevent-close menu.
 * </ul>
 */
final class GuiHandlers {

    private @Nullable Consumer<InventoryCloseEvent> closeHandler;
    private @Nullable Consumer<InventoryOpenEvent> openHandler;
    private @Nullable Consumer<InventoryClickEvent> defaultClickHandler;
    private @Nullable Consumer<InventoryClickEvent> outsideClickHandler;
    private boolean reopening;
    private boolean preventClose;
    private boolean closingProgrammatically;

    void onClose(Consumer<InventoryCloseEvent> handler) {
        this.closeHandler = Objects.requireNonNull(handler, "handler");
    }

    void onOpen(Consumer<InventoryOpenEvent> handler) {
        this.openHandler = Objects.requireNonNull(handler, "handler");
    }

    void onDefaultClick(Consumer<InventoryClickEvent> handler) {
        this.defaultClickHandler = Objects.requireNonNull(handler, "handler");
    }

    void onOutsideClick(Consumer<InventoryClickEvent> handler) {
        this.outsideClickHandler = Objects.requireNonNull(handler, "handler");
    }

    /** The empty-slot click fallback, for the click dispatcher. */
    @Nullable Consumer<InventoryClickEvent> defaultClick() {
        return defaultClickHandler;
    }

    /** The outside-the-menu click handler, for the click dispatcher. */
    @Nullable Consumer<InventoryClickEvent> outsideClick() {
        return outsideClickHandler;
    }

    /** Run the user's open handler, if one is set. */
    void fireOpen(InventoryOpenEvent event) {
        Consumer<InventoryOpenEvent> handler = openHandler;
        if (handler != null) {
            handler.accept(event);
        }
    }

    /** Run the user's close handler, if one is set. */
    void fireClose(InventoryCloseEvent event) {
        Consumer<InventoryCloseEvent> handler = closeHandler;
        if (handler != null) {
            handler.accept(event);
        }
    }

    /** Whether an internal title-change reopen is in progress, so close/open events are not user-visible. */
    boolean reopening() {
        return reopening;
    }

    /** Whether this menu reopens itself when a viewer tries to close it. */
    boolean preventsClose() {
        return preventClose;
    }

    void preventClose(boolean prevent) {
        this.preventClose = prevent;
    }

    /** Whether a deliberate API-driven close is in progress, which must bypass the prevent-close reopen. */
    boolean closingProgrammatically() {
        return closingProgrammatically;
    }

    /** Run {@code closeOp} with the programmatic-close flag set, so {@code handleClose} treats it as final. */
    void runProgrammaticClose(Runnable closeOp) {
        closingProgrammatically = true;
        try {
            closeOp.run();
        } finally {
            closingProgrammatically = false;
        }
    }

    /** Run {@code reopenOp} with the reopening flag set, so the close/open it causes stays internal. */
    void runReopen(Runnable reopenOp) {
        reopening = true;
        try {
            reopenOp.run();
        } finally {
            reopening = false;
        }
    }
}
