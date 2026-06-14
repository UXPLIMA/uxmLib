package com.uxplima.uxmlib.packet;

import java.util.Objects;

import io.papermc.paper.adventure.PaperAdventure;

/**
 * Converts an Adventure {@link net.kyori.adventure.text.Component} into the vanilla
 * {@link net.minecraft.network.chat.Component} that data-watcher values and packet fields expect. The
 * conversion itself is Paper's own {@link PaperAdventure} bridge; this class exists only to keep that single
 * {@code net.minecraft} touch in the quarantined packet module rather than scattered across renderers.
 */
public final class Components {

    private Components() {}

    /** Convert an Adventure component to the server's vanilla component. */
    public static net.minecraft.network.chat.Component asVanilla(net.kyori.adventure.text.Component component) {
        Objects.requireNonNull(component, "component");
        return PaperAdventure.asVanilla(component);
    }
}
