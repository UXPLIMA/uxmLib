package com.uxplima.uxmlib.packet;

import java.util.Objects;
import java.util.function.Consumer;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Builds a packet that exposes no usable public constructor (only an {@code Entity}-bound one and a private
 * buffer one) by writing its wire form into a fresh buffer and decoding it through the packet's public stream
 * codec. {@code ClientboundSetPassengersPacket} is the motivating case: there is no public way to seat raw
 * entity ids except through this round-trip.
 */
public final class Codecs {

    private Codecs() {}

    /**
     * Write the wire form via {@code writer} into a fresh buffer, decode it with {@code codec}, and return the
     * decoded value. The buffer is always released.
     *
     * @param codec the packet's public stream codec
     * @param writer writes the packet's fields into the buffer in wire order
     */
    public static Object decodeVia(StreamCodec<? super FriendlyByteBuf, ?> codec, Consumer<FriendlyByteBuf> writer) {
        Objects.requireNonNull(codec, "codec");
        Objects.requireNonNull(writer, "writer");
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            writer.accept(buffer);
            return Objects.requireNonNull(codec.decode(buffer), "decoded");
        } finally {
            buffer.release();
        }
    }
}
