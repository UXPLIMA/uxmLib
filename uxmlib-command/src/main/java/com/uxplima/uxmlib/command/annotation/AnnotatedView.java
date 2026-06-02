package com.uxplima.uxmlib.command.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The effective-annotations view of one reflective element (a class, method, or parameter): the annotations
 * actually declared on it, with any registered {@link AnnotationReplacer} already applied. A matched custom
 * annotation is removed and the core annotations it stands for are folded in, so a read through this view
 * ({@link #get(Class)} / {@link #isPresent(Class)}) sees the rewritten model the renderer should build from,
 * not the raw source. Built once per element by {@link #of(AnnotatedElement, Replacers)}; immutable
 * afterwards, so it can be captured into the command model and consulted on the run path.
 *
 * <p>A replacement never re-triggers replacers (a single pass, not a fixpoint): a replacer that emits a core
 * annotation does not have that output fed back through another replacer, matching Lamp's single-level rewrite
 * and keeping the pass terminating and easy to reason about.
 */
final class AnnotatedView {

    private final AnnotatedElement element;
    private final Map<Class<? extends Annotation>, Annotation> effective;

    private AnnotatedView(AnnotatedElement element, Map<Class<? extends Annotation>, Annotation> effective) {
        this.element = element;
        this.effective = effective;
    }

    /**
     * The effective view of {@code element} under {@code replacers}: every declared annotation, with a matched
     * one replaced by the core annotations its replacer returns. Where a replacement and a raw annotation share
     * a type, the raw one already declared on the element wins (a replacer never silently overwrites an
     * explicit core annotation), and among several replacements of the same type the first wins.
     */
    static AnnotatedView of(AnnotatedElement element, Replacers replacers) {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(replacers, "replacers");
        Map<Class<? extends Annotation>, Annotation> raw = new LinkedHashMap<>();
        for (Annotation declared : element.getDeclaredAnnotations()) {
            raw.put(declared.annotationType(), declared);
        }
        Map<Class<? extends Annotation>, Annotation> effective = new LinkedHashMap<>(raw);
        applyReplacements(element, replacers, raw, effective);
        return new AnnotatedView(element, effective);
    }

    private static void applyReplacements(
            AnnotatedElement element,
            Replacers replacers,
            Map<Class<? extends Annotation>, Annotation> raw,
            Map<Class<? extends Annotation>, Annotation> effective) {
        for (Annotation declared : raw.values()) {
            for (Annotation produced : replacers.replace(declared, element)) {
                Class<? extends Annotation> type = Objects.requireNonNull(
                                produced, "an AnnotationReplacer returned a null annotation")
                        .annotationType();
                // A raw annotation on the element, or an earlier replacement of the same type, is kept.
                if (!raw.containsKey(type)) {
                    effective.putIfAbsent(type, produced);
                }
            }
        }
    }

    /** The reflective element this view wraps, for messages and as the fallback identity in the model. */
    AnnotatedElement element() {
        return element;
    }

    /** The effective annotation of {@code type}, or {@code null} when neither declared nor produced. */
    <A extends Annotation> @Nullable A get(Class<A> type) {
        Objects.requireNonNull(type, "type");
        return type.cast(effective.get(type));
    }

    /** Whether an annotation of {@code type} is effectively present (declared or produced by a replacer). */
    boolean isPresent(Class<? extends Annotation> type) {
        Objects.requireNonNull(type, "type");
        return effective.containsKey(type);
    }
}
