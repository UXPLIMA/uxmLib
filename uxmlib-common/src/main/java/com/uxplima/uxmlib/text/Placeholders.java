package com.uxplima.uxmlib.text;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * A typed, lazy content-placeholder layer over a subject {@code T}. Each named placeholder maps to a
 * function of the subject; the function is invoked <em>only</em> when its tag actually appears in the rendered
 * template, so expensive lookups (a database read, an economy balance) never run for a tag a message does not
 * use. The output is a MiniMessage {@link TagResolver}, so it composes directly with
 * {@link Text#mini(String, TagResolver...)} and sits alongside the styling tags rather than replacing them.
 *
 * <p>This is a pure functional layer: it needs no Bukkit and never mutates the subject. Build one with
 * {@link #builder()}, register placeholders, then call {@link #resolver(Object)} per subject at render time.
 *
 * @param <T> the subject every placeholder reads from
 */
public final class Placeholders<T> {

    private final Map<String, Function<T, Tag>> entries;

    private Placeholders(Map<String, Function<T, Tag>> entries) {
        this.entries = entries;
    }

    /** Start building a placeholder set for subjects of type {@code T}. */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * A {@link TagResolver} that resolves this set's placeholders against {@code subject}. Each placeholder's
     * function is evaluated lazily, the first time its tag is encountered during a render; tags that never
     * appear in the template are never evaluated.
     */
    public TagResolver resolver(T subject) {
        Objects.requireNonNull(subject, "subject");
        TagResolver.Builder out = TagResolver.builder();
        for (Map.Entry<String, Function<T, Tag>> entry : entries.entrySet()) {
            Function<T, Tag> producer = entry.getValue();
            out.tag(entry.getKey(), (queue, ctx) -> producer.apply(subject));
        }
        return out.build();
    }

    /** Render {@code template} against {@code subject}, composing with any extra {@code resolvers}. */
    public Component render(String template, T subject, TagResolver... resolvers) {
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(resolvers, "resolvers");
        return Text.mini(template, TagResolver.resolver(resolver(subject), TagResolver.resolver(resolvers)));
    }

    /** Mutable builder for a {@link Placeholders} set; reusable and order-preserving. */
    public static final class Builder<T> {

        private final Map<String, Function<T, Tag>> entries = new LinkedHashMap<>();

        private Builder() {}

        /**
         * Register {@code key} as an <em>unparsed</em> placeholder: the produced string is inserted literally,
         * so any MiniMessage tags inside it are shown as text, never parsed. The safe default for untrusted or
         * user-provided values.
         */
        public Builder<T> add(String key, Function<? super T, String> value) {
            return put(key, value, raw -> Tag.inserting(Component.text(raw)));
        }

        /**
         * Register {@code key} as a <em>parsed</em> placeholder: the produced string is itself parsed as
         * MiniMessage. Use only for values you trust to contain markup.
         */
        public Builder<T> addParsed(String key, Function<? super T, String> value) {
            return put(key, value, raw -> Tag.inserting(Text.mini(raw)));
        }

        /** Register {@code key} as a placeholder that inserts an already-built {@link Component}. */
        public Builder<T> addComponent(String key, Function<? super T, Component> value) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(value, "value");
            entries.put(key, subject -> Tag.inserting(requireValue(key, value.apply(subject))));
            return this;
        }

        private Builder<T> put(String key, Function<? super T, String> value, Function<String, Tag> wrap) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(value, "value");
            entries.put(key, subject -> wrap.apply(requireValue(key, value.apply(subject))));
            return this;
        }

        /** Build an immutable snapshot; the builder may be reused afterwards. */
        public Placeholders<T> build() {
            return new Placeholders<>(new LinkedHashMap<>(entries));
        }
    }

    private static <V> V requireValue(String key, V produced) {
        return Objects.requireNonNull(produced, () -> "placeholder '" + key + "' produced a null value");
    }
}
