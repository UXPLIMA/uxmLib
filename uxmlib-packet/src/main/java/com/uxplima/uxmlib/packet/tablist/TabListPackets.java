package com.uxplima.uxmlib.packet.tablist;

import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;

/**
 * The seam between pure tab-list logic and the NMS packet construction. Every packet crosses this boundary as
 * an opaque {@link Object}, so this interface — and everything that depends on it — carries no
 * {@code net.minecraft} reference and stays unit-testable against a fake. The single implementation that builds
 * real {@code ClientboundPlayerInfoUpdatePacket}/{@code ClientboundPlayerInfoRemovePacket} packets against the
 * Mojang-mapped dev bundle lives behind this port in {@code tablist.internal}.
 *
 * <p>These packets are how you do the per-viewer tab things native Paper cannot: a custom display name, a
 * client-side sort order, and a custom skin for an entry you fully control. Each method returns one built
 * packet; {@link #send(Player, Object)} writes it to a viewer's connection, so the same packet can be sent to
 * many viewers without rebuilding it.
 */
public interface TabListPackets {

    /**
     * Build an add-or-update packet that introduces a fully-controlled {@link TabEntry} — profile (with the
     * skin when present), listed, display name, and list order. Use this for an entry you own; you would not
     * re-add a real online player this way.
     */
    Object addOrUpdate(TabEntry entry);

    /** Build a packet that changes only the display name of an existing entry. */
    Object displayName(UUID id, Component name);

    /** Build a packet that changes only the client-side sort order of an existing entry. */
    Object listOrder(UUID id, int order);

    /** Build a packet that removes the given profile ids from the tab list. */
    Object remove(List<UUID> ids);

    /** Write {@code packet} to {@code viewer}'s connection. A no-op if the connection cannot be resolved. */
    void send(Player viewer, Object packet);
}
