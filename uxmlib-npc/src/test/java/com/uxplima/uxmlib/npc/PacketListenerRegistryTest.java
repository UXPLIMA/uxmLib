package com.uxplima.uxmlib.npc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class PacketListenerRegistryTest {

    private static final UUID PLAYER = UUID.randomUUID();

    @Test
    void emptyRegistryReportsEmptyAndPasses() {
        PacketListenerRegistry registry = new PacketListenerRegistry();
        assertThat(registry.isEmpty()).isTrue();
        PacketListenerRegistry.Dispatch dispatch = registry.dispatch(PacketDirection.OUTBOUND, PLAYER, new Object());
        assertThat(dispatch.cancelled()).isFalse();
        assertThat(dispatch.faults()).isEmpty();
    }

    @Test
    void registerIsIdempotentForSameInstanceAndPreservesOrder() {
        PacketListenerRegistry registry = new PacketListenerRegistry();
        PacketListener a = (id, packet) -> PacketAction.PASS;
        PacketListener b = (id, packet) -> PacketAction.PASS;
        registry.register(a);
        registry.register(a);
        registry.register(b);
        assertThat(registry.snapshot()).containsExactly(a, b);
    }

    @Test
    void anySingleCancelVetoesButEveryListenerStillSeesThePacket() {
        PacketListenerRegistry registry = new PacketListenerRegistry();
        List<String> seen = new ArrayList<>();
        registry.register((id, packet) -> {
            seen.add("first");
            return PacketAction.CANCEL;
        });
        registry.register((id, packet) -> {
            seen.add("second");
            return PacketAction.PASS;
        });

        PacketListenerRegistry.Dispatch dispatch = registry.dispatch(PacketDirection.OUTBOUND, PLAYER, new Object());

        assertThat(dispatch.cancelled()).isTrue();
        assertThat(seen).containsExactly("first", "second");
    }

    @Test
    void inboundDispatchUsesOnReceiveDefaultPass() {
        PacketListenerRegistry registry = new PacketListenerRegistry();
        // Only overrides onSend; onReceive defaults to PASS, so an inbound dispatch must not cancel.
        registry.register((id, packet) -> PacketAction.CANCEL);

        PacketListenerRegistry.Dispatch dispatch = registry.dispatch(PacketDirection.INBOUND, PLAYER, new Object());

        assertThat(dispatch.cancelled()).isFalse();
    }

    @Test
    void inboundDispatchHonoursOnReceiveOverride() {
        PacketListenerRegistry registry = new PacketListenerRegistry();
        registry.register(new PacketListener() {
            @Override
            public PacketAction onSend(@Nullable UUID player, Object packet) {
                return PacketAction.PASS;
            }

            @Override
            public PacketAction onReceive(@Nullable UUID player, Object packet) {
                return PacketAction.CANCEL;
            }
        });

        PacketListenerRegistry.Dispatch dispatch = registry.dispatch(PacketDirection.INBOUND, PLAYER, new Object());

        assertThat(dispatch.cancelled()).isTrue();
    }

    @Test
    void aThrowingListenerIsFailOpenAndItsFaultIsCollected() {
        PacketListenerRegistry registry = new PacketListenerRegistry();
        RuntimeException boom = new IllegalStateException("boom");
        registry.register((id, packet) -> {
            throw boom;
        });
        registry.register((id, packet) -> PacketAction.PASS);

        PacketListenerRegistry.Dispatch dispatch = registry.dispatch(PacketDirection.OUTBOUND, PLAYER, new Object());

        assertThat(dispatch.cancelled()).isFalse();
        assertThat(dispatch.faults()).containsExactly(boom);
    }

    @Test
    void nullPlayerIsAllowed() {
        PacketListenerRegistry registry = new PacketListenerRegistry();
        List<@Nullable UUID> seen = new ArrayList<>();
        registry.register((id, packet) -> {
            seen.add(id);
            return PacketAction.PASS;
        });

        registry.dispatch(PacketDirection.OUTBOUND, null, new Object());

        assertThat(seen).containsExactly((UUID) null);
    }

    @Test
    void unregisterRemovesTheListener() {
        PacketListenerRegistry registry = new PacketListenerRegistry();
        PacketListener listener = (id, packet) -> PacketAction.PASS;
        registry.register(listener);
        assertThat(registry.unregister(listener)).isTrue();
        assertThat(registry.isEmpty()).isTrue();
        assertThat(registry.unregister(listener)).isFalse();
    }

    @Test
    void dispatchRejectsNullDirectionAndPacket() {
        PacketListenerRegistry registry = new PacketListenerRegistry();
        assertThatNullPointerException()
                .isThrownBy(() -> registry.dispatch(PacketDirection.OUTBOUND, PLAYER, nullPacket()));
    }

    @SuppressWarnings("NullAway") // intentionally feeds a null packet to assert the entry-point guard fires.
    private static Object nullPacket() {
        return null;
    }
}
