package com.uxplima.uxmlib.item;

import java.util.Objects;
import java.util.Optional;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import io.papermc.paper.persistence.PersistentDataContainerView;

import org.jspecify.annotations.Nullable;

/**
 * Typed convenience over a {@link PersistentDataContainer} that trims the {@link PersistentDataType} ceremony:
 * reads come back as an {@link Optional} (or via an explicit default) instead of a possibly-{@code null} value,
 * and the read side accepts Paper's {@link PersistentDataContainerView} so callers can pull keys off an item
 * without ever taking a mutable handle. The coercion-to-{@code Optional} idea follows rtag's {@code OptionalType}
 * and Item-NBT-API's typed accessors (both MIT).
 *
 * <p>Pair the read helpers with {@link Items#editPdc(ItemStack, java.util.function.Consumer)} for the write side:
 *
 * <pre>{@code
 * Items.editPdc(item, pdc -> Pdc.set(pdc, key, PersistentDataType.INTEGER, 7));
 * int coins = Pdc.read(item).getOrDefault(key, PersistentDataType.INTEGER, 0);
 * }</pre>
 */
public final class Pdc {

    private Pdc() {}

    /** Store {@code value} under {@code key} with the given type; replaces any existing value. */
    public static <P, C> void set(
            PersistentDataContainer pdc, NamespacedKey key, PersistentDataType<P, C> type, C value) {
        Objects.requireNonNull(pdc, "pdc");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(value, "value");
        pdc.set(key, type, value);
    }

    /** Whether {@code key} holds a value of {@code type}. */
    public static <P, C> boolean has(
            PersistentDataContainerView view, NamespacedKey key, PersistentDataType<P, C> type) {
        Objects.requireNonNull(view, "view");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(type, "type");
        return view.has(key, type);
    }

    /** The value under {@code key}, or an empty {@link Optional} when absent or of another type. */
    public static <P, C> Optional<C> get(
            PersistentDataContainerView view, NamespacedKey key, PersistentDataType<P, C> type) {
        Objects.requireNonNull(view, "view");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(type, "type");
        return Optional.ofNullable(view.get(key, type));
    }

    /** The value under {@code key}, or {@code def} when absent. {@code def} must not be {@code null}. */
    public static <P, C> C getOrDefault(
            PersistentDataContainerView view, NamespacedKey key, PersistentDataType<P, C> type, C def) {
        Objects.requireNonNull(view, "view");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(def, "def");
        return view.getOrDefault(key, type, def);
    }

    /** A read-only reader over {@code item}'s persistent data; never exposes a mutable container. */
    public static PdcReader read(ItemStack item) {
        Objects.requireNonNull(item, "item");
        return new PdcReader(item.getPersistentDataContainer());
    }

    /** A read-only reader over an already-acquired {@link PersistentDataContainerView}. */
    public static PdcReader read(PersistentDataContainerView view) {
        Objects.requireNonNull(view, "view");
        return new PdcReader(view);
    }

    /**
     * Read-only window over a {@link PersistentDataContainerView}. Holds no mutator, so handing one out lets a
     * caller inspect keys with the same typed ergonomics as {@link Pdc} while guaranteeing it cannot write.
     */
    public static final class PdcReader {

        private final PersistentDataContainerView view;

        private PdcReader(PersistentDataContainerView view) {
            this.view = view;
        }

        /** Whether {@code key} holds a value of {@code type}. */
        public <P, C> boolean has(NamespacedKey key, PersistentDataType<P, C> type) {
            return Pdc.has(view, key, type);
        }

        /** The value under {@code key}, or an empty {@link Optional} when absent or of another type. */
        public <P, C> Optional<C> get(NamespacedKey key, PersistentDataType<P, C> type) {
            return Pdc.get(view, key, type);
        }

        /** The value under {@code key}, or {@code def} when absent. {@code def} must not be {@code null}. */
        public <P, C> C getOrDefault(NamespacedKey key, PersistentDataType<P, C> type, C def) {
            return Pdc.getOrDefault(view, key, type, def);
        }

        /** Convenience overload resolving {@code key} via {@link NamespacedKey#fromString(String)}. */
        public <P, C> C getOrDefault(String key, PersistentDataType<P, C> type, C def) {
            return getOrDefault(requireKey(key), type, def);
        }

        private static NamespacedKey requireKey(String key) {
            Objects.requireNonNull(key, "key");
            NamespacedKey parsed = NamespacedKey.fromString(key);
            if (parsed == null) {
                throw new IllegalArgumentException("not a valid namespaced key: " + key);
            }
            return parsed;
        }

        @Override
        public boolean equals(@Nullable Object other) {
            return other instanceof PdcReader reader && view.equals(reader.view);
        }

        @Override
        public int hashCode() {
            return view.hashCode();
        }
    }
}
