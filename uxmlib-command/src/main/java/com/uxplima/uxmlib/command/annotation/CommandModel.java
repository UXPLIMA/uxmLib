package com.uxplima.uxmlib.command.annotation;

import java.util.List;
import java.util.Objects;

import com.uxplima.uxmlib.command.annotation.annotations.Command;
import com.uxplima.uxmlib.command.annotation.annotations.Permission;
import org.jspecify.annotations.Nullable;

/**
 * The platform-neutral intermediate representation of a whole command: the root label, its aliases and
 * description, whether to auto-generate help, an optional class-level permission, and the ordered list of
 * {@link BranchModel}s. {@code CommandModels} produces it by reflecting an {@code @}{@link Command} handler;
 * {@code BrigadierRenderer} consumes it to emit the Brigadier tree. Nothing here knows about Brigadier, so a
 * second renderer (a Discord slash surface, a {@code /help} generator) can target the same model.
 */
final class CommandModel {

    private final Object handler;
    private final Command command;
    private final AnnotatedView classView;
    private final @Nullable Permission classPermission;
    private final List<BranchModel> branches;

    CommandModel(
            Object handler,
            Command command,
            AnnotatedView classView,
            @Nullable Permission classPermission,
            List<BranchModel> branches) {
        this.handler = Objects.requireNonNull(handler, "handler");
        this.command = Objects.requireNonNull(command, "command");
        this.classView = Objects.requireNonNull(classView, "classView");
        this.classPermission = classPermission;
        this.branches = List.copyOf(Objects.requireNonNull(branches, "branches"));
    }

    /** The effective annotation view of the {@code @Command} class, carrying its replacer-rewritten annotations. */
    AnnotatedView classView() {
        return classView;
    }

    /** The handler instance whose methods the branches invoke. */
    Object handler() {
        return handler;
    }

    /** The {@code @Command} annotation carrying the root label, aliases, description, and help flag. */
    Command command() {
        return command;
    }

    /** The root command label. */
    String name() {
        return command.name();
    }

    /** The class-level permission gating the whole root command, or {@code null} when there is none. */
    @Nullable Permission classPermission() {
        return classPermission;
    }

    /** The branches of this command, ordered longest-literal-path first for deterministic attachment. */
    List<BranchModel> branches() {
        return branches;
    }
}
