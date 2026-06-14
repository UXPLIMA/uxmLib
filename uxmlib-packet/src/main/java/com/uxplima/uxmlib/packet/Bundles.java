package com.uxplima.uxmlib.packet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;

/**
 * Wraps several already-built clientbound game packets into a single {@link ClientboundBundlePacket}, so a
 * renderer can deliver a spawn/metadata/mount sequence as one atomic frame. Inputs are typed {@link Object} so
 * the calling renderers never need to reference {@code net.minecraft} themselves; the unchecked cast is safe
 * because the only packets ever fed in are the clientbound game packets the renderers build through this module.
 */
public final class Bundles {

    private Bundles() {}

    /** Wrap {@code packets} into one {@link ClientboundBundlePacket}. */
    public static Object of(List<Object> packets) {
        Objects.requireNonNull(packets, "packets");
        List<Packet<? super ClientGamePacketListener>> game = new ArrayList<>(packets.size());
        for (Object packet : packets) {
            game.add(asGamePacket(packet));
        }
        return new ClientboundBundlePacket(game);
    }

    @SuppressWarnings("unchecked") // The renderers only ever feed this clientbound game packets built here.
    private static Packet<? super ClientGamePacketListener> asGamePacket(Object packet) {
        return (Packet<? super ClientGamePacketListener>) Objects.requireNonNull(packet, "packet");
    }
}
