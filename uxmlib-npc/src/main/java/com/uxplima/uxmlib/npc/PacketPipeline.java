package com.uxplima.uxmlib.npc;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.entity.Player;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import org.jspecify.annotations.Nullable;

/**
 * Injects and ejects a {@link PacketInterceptor} into a player's connection pipeline, and re-asserts its
 * position with a {@link PipelineWatchdog} pass when other plugins splice ahead of it.
 *
 * <p>Anchoring is by handler <em>name</em>, not index: our duplex handler is added immediately after the
 * vanilla {@code "decoder"} (so it sees fully-formed packet objects in both directions) under a stable name
 * derived from {@code handlerName}. Every pipeline mutation is idempotent — a re-inject that finds the
 * handler already present is a no-op, and an eject that finds it gone is silent — so join/quit/reorder
 * choreography can call these freely without double-add or double-remove crashes.
 *
 * <p>This class performs no scheduling itself; the inject-on-join / delayed-reorder choreography belongs to
 * the caller (via the library {@code Scheduler}), keeping this class a pure pipeline mutator that is easy to
 * reason about. It never throws on a missing channel — a player whose channel cannot be resolved (e.g. a mock
 * player under test) simply yields {@code false} from {@link #inject} / {@link #isInjected}.
 */
public final class PacketPipeline {

    /** The vanilla handler our duplex handler sits immediately after; stable across 1.21.x. */
    public static final String DEFAULT_ANCHOR = "decoder";

    private final ChannelResolver resolver;
    private final PacketListenerRegistry registry;
    private final String handlerName;
    private final PipelineWatchdog watchdog;
    private final Consumer<Throwable> faultSink;

    /**
     * @param resolver reaches the player's Netty channel
     * @param registry the listeners every injected interceptor dispatches through
     * @param handlerName the unique pipeline name our handler registers under (namespace it per plugin)
     * @param faultSink receives listener throwables collected on the I/O thread, to log off-thread
     */
    public PacketPipeline(
            ChannelResolver resolver,
            PacketListenerRegistry registry,
            String handlerName,
            Consumer<Throwable> faultSink) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.handlerName = requireNonBlank(handlerName);
        this.watchdog = new PipelineWatchdog(this.handlerName, DEFAULT_ANCHOR);
        this.faultSink = Objects.requireNonNull(faultSink, "faultSink");
    }

    public String handlerName() {
        return handlerName;
    }

    /**
     * Inject our interceptor into {@code player}'s pipeline, immediately after the {@code "decoder"} anchor.
     * Idempotent. Returns {@code false} (no throw) if the channel cannot be resolved or the anchor is absent.
     */
    public boolean inject(Player player) {
        Objects.requireNonNull(player, "player");
        Optional<Channel> channel = resolver.resolve(player);
        return channel.isPresent() && inject(channel.orElseThrow(), player.getUniqueId());
    }

    private boolean inject(Channel channel, UUID owner) {
        ChannelPipeline pipeline = channel.pipeline();
        if (pipeline.get(handlerName) != null) {
            return true; // already injected; idempotent
        }
        if (pipeline.get(DEFAULT_ANCHOR) == null) {
            return false; // unexpected pipeline shape; do not guess a position
        }
        PacketInterceptor interceptor = new PacketInterceptor(registry, () -> owner, faultSink);
        pipeline.addAfter(DEFAULT_ANCHOR, handlerName, interceptor);
        return true;
    }

    /** Remove our interceptor from {@code player}'s pipeline. Idempotent and {@link NoSuchElementException}-safe. */
    public boolean eject(Player player) {
        Objects.requireNonNull(player, "player");
        Optional<Channel> channel = resolver.resolve(player);
        return channel.isPresent() && eject(channel.orElseThrow());
    }

    private boolean eject(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        if (pipeline.get(handlerName) == null) {
            return false;
        }
        try {
            pipeline.remove(handlerName);
            return true;
        } catch (NoSuchElementException raced) {
            return false; // another thread removed it between the guard and the remove; fine.
        }
    }

    /** {@code true} if our interceptor is currently present in {@code player}'s pipeline. */
    public boolean isInjected(Player player) {
        Objects.requireNonNull(player, "player");
        return resolver.resolve(player)
                .map(channel -> channel.pipeline().get(handlerName) != null)
                .orElse(false);
    }

    /**
     * Run one self-healing pass: if our handler has drifted out of position, move it back immediately after
     * the anchor. Returns the {@link PipelineWatchdog.Decision} that was applied (or merely observed). Safe to
     * call repeatedly; a no-op when nothing changed.
     */
    public PipelineWatchdog.Decision reorder(Player player) {
        Objects.requireNonNull(player, "player");
        Optional<Channel> channel = resolver.resolve(player);
        return channel.map(this::reorder).orElse(PipelineWatchdog.Decision.MISSING);
    }

    private PipelineWatchdog.Decision reorder(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        List<String> names = pipeline.names();
        PipelineWatchdog.Decision decision = watchdog.evaluate(names);
        if (decision.needsReorder()) {
            applyReorder(pipeline);
        }
        return decision;
    }

    private void applyReorder(ChannelPipeline pipeline) {
        try {
            @Nullable ChannelHandler handler = pipeline.get(handlerName);
            if (handler == null) {
                return;
            }
            pipeline.remove(handlerName);
            pipeline.addAfter(DEFAULT_ANCHOR, handlerName, handler);
        } catch (NoSuchElementException raced) {
            // The anchor or our handler vanished mid-reorder (another plugin mutating concurrently). The next
            // watchdog pass re-evaluates from scratch, so dropping this one is safe.
        }
    }

    private static String requireNonBlank(@Nullable String value) {
        Objects.requireNonNull(value, "handlerName");
        if (value.isBlank()) {
            throw new IllegalArgumentException("handlerName must not be blank");
        }
        return value;
    }
}
