package com.uxplima.uxmlib.command.annotation;

import java.util.Objects;

import com.uxplima.uxmlib.command.annotation.annotations.Secret;

/**
 * The single rule for whether a branch is {@code @}{@link Secret}: the branch method effectively carries it,
 * or its declaring {@code @Command} class does. Factored out so the help renderer and any future listing
 * surface share one definition rather than each re-deriving it, the way the cooldown gate folds its annotation
 * lookup into one place. Reads the branch's effective annotation views, so a replacer that synthesises
 * {@code @Secret} hides the branch from help just like a declared one.
 */
final class Secrets {

    private Secrets() {}

    /** Whether {@code branch} (its method or its declaring class) is effectively annotated {@code @}{@link Secret}. */
    static boolean isSecret(BranchModel branch) {
        Objects.requireNonNull(branch, "branch");
        return branch.methodView().isPresent(Secret.class) || branch.classView().isPresent(Secret.class);
    }
}
