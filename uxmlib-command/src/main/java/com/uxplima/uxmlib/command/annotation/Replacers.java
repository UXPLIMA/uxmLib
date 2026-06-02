package com.uxplima.uxmlib.command.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The immutable set of {@link AnnotationReplacer}s a {@link ParamResolvers} carries, keyed by the custom
 * annotation type each consumes. {@link AnnotatedView} dispatches every declared annotation through
 * {@link #replace} while assembling an element's effective view; an annotation with no registered replacer
 * yields nothing and is kept as-is. Holding the registry separate from {@link ParamResolvers} keeps that
 * class within its size budget and gives the view one collaborator to ask.
 */
final class Replacers {

    private static final Replacers NONE = new Replacers(Map.of());

    private final Map<Class<? extends Annotation>, AnnotationReplacer<?>> byType;

    private Replacers(Map<Class<? extends Annotation>, AnnotationReplacer<?>> byType) {
        this.byType = byType;
    }

    /** The empty registry: no annotation is ever rewritten. */
    static Replacers none() {
        return NONE;
    }

    /** This registry with {@code replacer} registered for {@code type}, replacing any prior one for that type. */
    <A extends Annotation> Replacers with(Class<A> type, AnnotationReplacer<A> replacer) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(replacer, "replacer");
        Map<Class<? extends Annotation>, AnnotationReplacer<?>> next = new HashMap<>(byType);
        next.put(type, replacer);
        return new Replacers(Map.copyOf(next));
    }

    /** Whether any replacer is registered; lets the view skip the per-annotation dispatch entirely when empty. */
    boolean isEmpty() {
        return byType.isEmpty();
    }

    /**
     * The core annotations {@code declared} stands for on {@code element}, or an empty list when no replacer is
     * registered for its type. The replacer's own contract forbids a {@code null} return; a replacer that
     * breaks it surfaces here as a {@link CommandParseException} naming the offending annotation rather than a
     * raw {@code NullPointerException} deep in the build.
     */
    List<Annotation> replace(Annotation declared, AnnotatedElement element) {
        AnnotationReplacer<?> replacer = byType.get(declared.annotationType());
        if (replacer == null) {
            return List.of();
        }
        List<Annotation> produced = dispatch(replacer, declared, element);
        if (produced == null) {
            throw new CommandParseException(
                    "AnnotationReplacer for @" + declared.annotationType().getSimpleName()
                            + " returned null; return an empty list to contribute nothing");
        }
        return produced;
    }

    @SuppressWarnings("unchecked") // a replacer keyed by type A only ever sees an annotation of type A
    private static @Nullable List<Annotation> dispatch(
            AnnotationReplacer<?> replacer, Annotation declared, AnnotatedElement element) {
        return ((AnnotationReplacer<Annotation>) replacer).replace(declared, element);
    }
}
