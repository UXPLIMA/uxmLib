package com.uxplima.uxmlib.npc;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jspecify.annotations.Nullable;

/**
 * An ordered, thread-safe set of {@link PacketListener}s and the dispatch that folds their verdicts into one.
 *
 * <p>Registration order is preserved (listeners run first-registered-first). Backed by a
 * {@link CopyOnWriteArrayList} so dispatch from the Netty I/O thread never blocks registration from the main
 * thread and a snapshot is iterated lock-free. Dispatch is <b>fail-open</b>: a listener that throws is
 * treated as a {@link PacketAction#PASS} and the throwable is collected so the caller can log it off the I/O
 * thread, never swallowed silently in a way that loses the cause.
 *
 * <p>The folding rule is a veto: the packet is cancelled if <em>any</em> listener returns
 * {@link PacketAction#CANCEL}. Every listener still sees the packet even after one cancels, so listeners that
 * only observe stay correct.
 */
public final class PacketListenerRegistry {

    private final CopyOnWriteArrayList<PacketListener> listeners = new CopyOnWriteArrayList<>();

    /** Register {@code listener} at the end of the dispatch order. A duplicate instance is not added twice. */
    public void register(PacketListener listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.addIfAbsent(listener);
    }

    /** Remove {@code listener}; returns {@code true} if it was present. */
    public boolean unregister(PacketListener listener) {
        Objects.requireNonNull(listener, "listener");
        return listeners.remove(listener);
    }

    /** {@code true} if no listener is registered (the interceptor can then forward without dispatching). */
    public boolean isEmpty() {
        return listeners.isEmpty();
    }

    /** An immutable snapshot of the current listeners in dispatch order. */
    public List<PacketListener> snapshot() {
        return List.copyOf(listeners);
    }

    /**
     * Dispatch {@code packet} (travelling {@code direction} for the connection owned by {@code player}) to
     * every listener and fold the verdicts. Never throws: a listener fault is folded as {@code PASS} and
     * attached to the result.
     */
    public Dispatch dispatch(PacketDirection direction, @Nullable UUID player, Object packet) {
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(packet, "packet");
        boolean cancel = false;
        @Nullable List<Throwable> faults = null;
        for (PacketListener listener : listeners) {
            try {
                PacketAction action = invoke(listener, direction, player, packet);
                if (action.cancels()) {
                    cancel = true;
                }
            } catch (RuntimeException fault) {
                faults = record(faults, fault);
            }
        }
        return new Dispatch(cancel ? PacketAction.CANCEL : PacketAction.PASS, faults == null ? List.of() : faults);
    }

    private static PacketAction invoke(
            PacketListener listener, PacketDirection direction, @Nullable UUID player, Object packet) {
        return direction == PacketDirection.OUTBOUND
                ? listener.onSend(player, packet)
                : listener.onReceive(player, packet);
    }

    private static List<Throwable> record(@Nullable List<Throwable> faults, Throwable fault) {
        List<Throwable> sink = faults == null ? new java.util.ArrayList<>() : faults;
        sink.add(fault);
        return sink;
    }

    /**
     * The folded outcome of a dispatch: the verdict and any listener faults collected along the way (so the
     * caller logs them off the I/O thread).
     */
    public record Dispatch(PacketAction action, List<Throwable> faults) {

        public Dispatch {
            Objects.requireNonNull(action, "action");
            faults = List.copyOf(faults);
        }

        /** {@code true} if the folded verdict cancels the packet. */
        public boolean cancelled() {
            return action.cancels();
        }
    }
}
