package com.uxplima.uxmlib.npc;

/**
 * The interception seam. A listener inspects a raw packet object (an opaque {@link Object}: this foundation
 * does not yet decode packets into typed wrappers) flowing through a player's connection channel and decides
 * whether to {@link PacketAction#PASS} or {@link PacketAction#CANCEL} it.
 *
 * <p>Both callbacks run on a <b>Netty I/O thread</b>, never the main server thread. Implementations must not
 * touch the Bukkit API directly here; hand work to a {@code Scheduler} if it must run on a region thread. A
 * listener must be cheap and must not block. If a callback throws, the registry swallows it and the packet
 * passes (fail-open), so a faulty listener can never break a player's connection.
 *
 * <p>The default methods return {@link PacketAction#PASS}, so a listener only overrides the direction it
 * cares about.
 */
@FunctionalInterface
public interface PacketListener {

    /**
     * Inspect an outbound (server-to-client) packet.
     *
     * @param player the channel owner's unique id, or {@code null} before the login handshake names the
     *     connection (the channel exists before a {@code Player} does)
     * @param packet the raw packet object; never {@code null}
     * @return the verdict for this packet
     */
    PacketAction onSend(java.util.@org.jspecify.annotations.Nullable UUID player, Object packet);

    /**
     * Inspect an inbound (client-to-server) packet. Defaults to {@link PacketAction#PASS}.
     *
     * @param player the channel owner's unique id, or {@code null} before login
     * @param packet the raw packet object; never {@code null}
     * @return the verdict for this packet
     */
    default PacketAction onReceive(java.util.@org.jspecify.annotations.Nullable UUID player, Object packet) {
        return PacketAction.PASS;
    }
}
