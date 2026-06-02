package com.uxplima.uxmlib.gui;

import java.util.List;
import java.util.Objects;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Applies a list of {@link GuiResponse}s — the declarative result of a click handler — in order. This is
 * the single place where a declarative click touches Bukkit, so handlers can stay pure functions and the
 * side effects live here, on the viewer's region thread.
 *
 * <p>Splitting application out of {@link GuiResponse} keeps the response model a small value type and the
 * "how to apply each kind" switch in one home that mirrors the sealed set, so adding a response kind forces
 * a matching application branch.
 */
final class GuiResponses {

    private GuiResponses() {}

    /** Apply every response in {@code responses} to {@code gui}/{@code event} in order. */
    static void apply(List<GuiResponse> responses, Gui gui, InventoryClickEvent event) {
        Objects.requireNonNull(responses, "responses");
        Objects.requireNonNull(gui, "gui");
        Objects.requireNonNull(event, "event");
        for (GuiResponse response : responses) {
            apply(Objects.requireNonNull(response, "response"), gui, event);
        }
    }

    private static void apply(GuiResponse response, Gui gui, InventoryClickEvent event) {
        switch (response) {
            case GuiResponse.Close ignored -> event.getWhoClicked().closeInventory();
            case GuiResponse.Open open -> open(open.gui(), event);
            case GuiResponse.Refresh ignored -> gui.refresh();
            case GuiResponse.UpdateItem update -> gui.set(update.slot(), update.item());
            case GuiResponse.ReplaceCursor replace -> event.getView().setCursor(replace.cursor());
            case GuiResponse.PlaySound play -> event.getWhoClicked().playSound(play.sound());
            case GuiResponse.Run run -> run.task().run();
            case GuiResponse.Nothing ignored -> {
                // Deliberately no effect: a handler that opts out still returns a response.
            }
        }
    }

    private static void open(Gui target, InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            target.open(player);
        }
    }
}
