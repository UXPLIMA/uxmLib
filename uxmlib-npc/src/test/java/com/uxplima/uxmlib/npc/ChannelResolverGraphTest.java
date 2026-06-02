package com.uxplima.uxmlib.npc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

/**
 * Drives the reflective object-graph walk directly with synthetic handle objects (MockBukkit has no
 * {@code getHandle()} chain to walk). Real {@link EmbeddedChannel}s stand in for the connection channel so the
 * anchor-preference and container-unwrapping behaviour can be asserted without a live server.
 */
class ChannelResolverGraphTest {

    private final ChannelResolver resolver = new ChannelResolver();

    private static EmbeddedChannel withAnchor() {
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(PacketPipeline.DEFAULT_ANCHOR, new ChannelInboundHandlerAdapter());
        return channel;
    }

    @Test
    void prefersTheChannelWhosePipelineCarriesTheAnchorOverAnyOtherChannel() {
        EmbeddedChannel decoy = new EmbeddedChannel(); // open, but no "decoder" — must not be chosen
        EmbeddedChannel gameplay = withAnchor();
        // Field order is deliberate: the decoy is declared first, so a blind first-Channel walk would pick it.
        Object handle = new TwoChannelHandle(decoy, gameplay);

        assertThat(resolver.findChannel(handle)).isSameAs(gameplay);
    }

    @Test
    void fallsBackToTheFirstOpenChannelWhenNoneCarriesTheAnchor() {
        EmbeddedChannel only = new EmbeddedChannel(); // open but anchorless (e.g. early handshake)
        Object handle = new CollectionHandle(List.of(only));

        assertThat(resolver.findChannel(handle)).isSameAs(only);
    }

    @Test
    void reachesAChannelHeldBehindAnAtomicReference() {
        EmbeddedChannel channel = withAnchor();
        Object handle = new AtomicHandle(new AtomicReference<>(channel));

        assertThat(resolver.findChannel(handle)).isSameAs(channel);
    }

    @Test
    void reachesAChannelHeldInsideACollection() {
        EmbeddedChannel channel = withAnchor();
        Object handle = new CollectionHandle(List.of(channel));

        assertThat(resolver.findChannel(handle)).isSameAs(channel);
    }

    private static final class TwoChannelHandle {
        @SuppressWarnings("unused")
        private final Channel first;

        @SuppressWarnings("unused")
        private final Connection connection;

        TwoChannelHandle(Channel first, Channel second) {
            this.first = first;
            this.connection = new Connection(second);
        }
    }

    private static final class Connection {
        @SuppressWarnings("unused")
        private final Channel channel;

        Connection(Channel channel) {
            this.channel = channel;
        }
    }

    private static final class AtomicHandle {
        @SuppressWarnings("unused")
        private final AtomicReference<Channel> ref;

        AtomicHandle(AtomicReference<Channel> ref) {
            this.ref = ref;
        }
    }

    private static final class CollectionHandle {
        @SuppressWarnings("unused")
        private final List<Channel> channels;

        CollectionHandle(List<Channel> channels) {
            this.channels = channels;
        }
    }
}
