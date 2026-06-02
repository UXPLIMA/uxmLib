package com.uxplima.uxmlib.npc;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

import org.bukkit.entity.Player;

import io.netty.channel.Channel;
import org.jspecify.annotations.Nullable;

/**
 * The one class that holds the unavoidable NMS reflection. It reaches a player's raw Netty
 * {@link Channel} via {@code CraftPlayer.getHandle() -> connection -> channel}, walking by field
 * <em>type</em> rather than obfuscated field <em>name</em> so it survives Mojang's remapping across 1.21.x.
 * Every step is guarded; the whole thing returns an {@link Optional} and <b>never throws</b> — on any mock
 * player, security manager, or layout change it simply returns {@link Optional#empty()}.
 *
 * <p>Why so much reflection: Paper exposes no public handle to the connection's Netty channel, and a
 * from-scratch (MIT) packet layer cannot use PacketEvents (GPL) to get it. Isolating it in this one class
 * keeps the rest of the module free of NMS. Every {@code setAccessible}/read is guarded and fails closed.
 */
public final class ChannelResolver {

    /** How deep into nested connection objects we search for a Channel field before giving up. */
    private static final int MAX_DEPTH = 4;

    /**
     * Resolve the Netty channel backing {@code player}'s connection.
     *
     * @return the channel, or empty if the player is not a CraftPlayer / the layout could not be walked / the
     *     channel is not open
     */
    public Optional<Channel> resolve(Player player) {
        Objects.requireNonNull(player, "player");
        @Nullable Object handle = invokeNoArg(player, "getHandle");
        if (handle == null) {
            return Optional.empty();
        }
        @Nullable Channel channel = findChannel(handle, MAX_DEPTH);
        return channel != null && channel.isOpen() ? Optional.of(channel) : Optional.empty();
    }

    /**
     * Depth-first search for a {@link Channel}-typed field reachable from {@code root}. A direct field is
     * preferred; otherwise we descend into each reference field once (bounded by {@code depth}) to reach the
     * connection's network manager where the channel actually lives.
     */
    private @Nullable Channel findChannel(Object root, int depth) {
        for (Field field : declaredFields(root.getClass())) {
            if (Channel.class.isAssignableFrom(field.getType())) {
                @Nullable Object value = read(field, root);
                if (value instanceof Channel channel) {
                    return channel;
                }
            }
        }
        if (depth <= 0) {
            return null;
        }
        return descend(root, depth);
    }

    private @Nullable Channel descend(Object root, int depth) {
        for (Field field : declaredFields(root.getClass())) {
            if (field.getType().isPrimitive() || field.getType().getName().startsWith("java.")) {
                continue;
            }
            @Nullable Object value = read(field, root);
            if (value == null || value == root) {
                continue;
            }
            @Nullable Channel found = findChannel(value, depth - 1);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static Field[] declaredFields(Class<?> type) {
        try {
            return type.getDeclaredFields();
        } catch (SecurityException ignored) {
            return new Field[0];
        }
    }

    private static @Nullable Object read(Field field, Object owner) {
        try {
            field.setAccessible(true);
            return field.get(owner);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // RuntimeException covers SecurityException/InaccessibleObjectException; on any of them we fail
            // closed and the channel simply resolves to empty.
            return null;
        }
    }

    private static @Nullable Object invokeNoArg(Object target, String method) {
        try {
            Method m = target.getClass().getMethod(method);
            m.setAccessible(true);
            return m.invoke(target);
        } catch (NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException
                | RuntimeException ignored) {
            // RuntimeException covers SecurityException/InaccessibleObjectException; fail closed to empty.
            return null;
        }
    }
}
