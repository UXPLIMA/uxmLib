package com.uxplima.uxmlib.config;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

/**
 * A HOCON-backed configuration. The file is parsed once on {@link #load(Path)} and held in an
 * {@link AtomicReference}; {@link #reload()} re-reads it and swaps the reference whole, so a reader sees
 * either the entire old tree or the entire new one — never a half-applied config.
 *
 * <p>Dotted paths ({@code "storage.host"}) address nested nodes for the typed scalar reads. Whole
 * subtrees map onto {@code @ConfigSerializable} types via {@link #get(Class)} / {@link #getNode(String,
 * Class, Object)}, so config can be modelled as records and classes rather than scattered string lookups.
 * A missing file yields an empty tree, so every scalar read returns its fallback.
 */
public final class HoconConfig {

    private final HoconConfigurationLoader loader;
    private final AtomicReference<CommentedConfigurationNode> root;

    private HoconConfig(HoconConfigurationLoader loader, CommentedConfigurationNode root) {
        this.loader = loader;
        this.root = new AtomicReference<>(root);
    }

    /** Load {@code file} as HOCON. A non-existent file loads as an empty tree. */
    public static HoconConfig load(Path file) {
        Objects.requireNonNull(file, "file");
        HoconConfigurationLoader loader =
                HoconConfigurationLoader.builder().path(file).build();
        return new HoconConfig(loader, read(loader));
    }

    /** Re-read the file and swap the in-memory tree atomically. */
    public void reload() {
        root.set(read(loader));
    }

    /** Write the current in-memory tree back to the file. */
    public void save() {
        try {
            loader.save(root.get());
        } catch (ConfigurateException failure) {
            throw new ConfigException("failed to save config", failure);
        }
    }

    /** The boolean at {@code path}, or {@code fallback} when absent. */
    public boolean getBoolean(String path, boolean fallback) {
        return node(path).getBoolean(fallback);
    }

    /** The int at {@code path}, or {@code fallback} when absent. */
    public int getInt(String path, int fallback) {
        return node(path).getInt(fallback);
    }

    /** The long at {@code path}, or {@code fallback} when absent. */
    public long getLong(String path, long fallback) {
        return node(path).getLong(fallback);
    }

    /** The double at {@code path}, or {@code fallback} when absent. */
    public double getDouble(String path, double fallback) {
        return node(path).getDouble(fallback);
    }

    /** The string at {@code path}, or {@code fallback} when absent. */
    public String getString(String path, String fallback) {
        Objects.requireNonNull(fallback, "fallback");
        String value = node(path).getString(fallback);
        return value != null ? value : fallback;
    }

    /** The string at {@code path}, empty when absent. */
    public Optional<String> getString(String path) {
        return Optional.ofNullable(node(path).getString());
    }

    /** Map the whole config onto {@code type} (a {@code @ConfigSerializable} record or class). */
    public <T> Optional<T> get(Class<T> type) {
        Objects.requireNonNull(type, "type");
        try {
            return Optional.ofNullable(currentRoot().get(type));
        } catch (SerializationException failure) {
            throw new ConfigException("failed to map config to " + type.getName(), failure);
        }
    }

    /** Map the subtree at {@code path} onto {@code type}, or return {@code fallback} when absent. */
    public <T> T getNode(String path, Class<T> type, T fallback) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(fallback, "fallback");
        try {
            T value = node(path).get(type);
            return value != null ? value : fallback;
        } catch (SerializationException failure) {
            throw new ConfigException("failed to map " + path + " to " + type.getName(), failure);
        }
    }

    /** The raw root node, for callers that need Configurate directly. */
    public CommentedConfigurationNode root() {
        return currentRoot();
    }

    private ConfigurationNode node(String path) {
        Objects.requireNonNull(path, "path");
        Object[] segments = path.split("\\.");
        return currentRoot().node(segments);
    }

    // The reference is seeded in the constructor and only ever swapped with a non-null node, so this is
    // never null at runtime; the requireNonNull tells NullAway what the AtomicReference cannot express.
    private CommentedConfigurationNode currentRoot() {
        return Objects.requireNonNull(root.get(), "root");
    }

    private static CommentedConfigurationNode read(HoconConfigurationLoader loader) {
        try {
            return loader.load();
        } catch (ConfigurateException failure) {
            throw new ConfigException("failed to load config", failure);
        }
    }
}
