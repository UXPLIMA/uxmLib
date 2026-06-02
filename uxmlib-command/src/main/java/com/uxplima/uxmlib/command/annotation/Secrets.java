package com.uxplima.uxmlib.command.annotation;

import java.lang.reflect.Method;
import java.util.Objects;

import com.uxplima.uxmlib.command.annotation.annotations.Secret;

/**
 * The single rule for whether a branch is {@code @}{@link Secret}: the method carries it, or its declaring
 * {@code @Command} class does. Factored out so the help renderer and any future listing surface share one
 * definition rather than each re-deriving it, the way the cooldown gate folds its annotation lookup into one
 * place.
 */
final class Secrets {

    private Secrets() {}

    /** Whether {@code method} (or its declaring class) is annotated {@code @}{@link Secret}. */
    static boolean isSecret(Method method) {
        Objects.requireNonNull(method, "method");
        return method.isAnnotationPresent(Secret.class)
                || method.getDeclaringClass().isAnnotationPresent(Secret.class);
    }
}
