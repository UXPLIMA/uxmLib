package com.uxplima.uxmlib.command.annotation;

import java.lang.reflect.Parameter;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * One flag or switch in the command model. A <em>value flag</em> ({@code @}{@link
 * com.uxplima.uxmlib.command.annotation.annotations.Flag}) reads {@code --name <value>}; a <em>switch</em>
 * ({@code @}{@link com.uxplima.uxmlib.command.annotation.annotations.Switch}) is a boolean presence flag
 * ({@code --name}, true when given). Both are position-independent and parsed out of a single greedy
 * trailing argument by {@link FlagArgumentType}; {@link ArgBinder} binds the parsed value to the parameter.
 * The {@code resolver} (null for a switch) is opaque so it can carry a {@link ParamResolver}.
 */
final class FlagModel {

    private final String name;
    private final char shorthand;
    private final boolean valueFlag;
    private final @Nullable ParamResolver<?> resolver;
    private final Parameter parameter;

    private FlagModel(
            String name, char shorthand, boolean valueFlag, @Nullable ParamResolver<?> resolver, Parameter parameter) {
        this.name = Objects.requireNonNull(name, "name");
        this.shorthand = shorthand;
        this.valueFlag = valueFlag;
        this.resolver = resolver;
        this.parameter = Objects.requireNonNull(parameter, "parameter");
    }

    /** A value flag named {@code name} (optional {@code shorthand}, 0 for none) resolved by {@code resolver}. */
    static FlagModel valueFlag(String name, char shorthand, ParamResolver<?> resolver, Parameter parameter) {
        return new FlagModel(name, shorthand, true, Objects.requireNonNull(resolver, "resolver"), parameter);
    }

    /** A boolean presence switch named {@code name} (optional {@code shorthand}, 0 for none). */
    static FlagModel switchFlag(String name, char shorthand, Parameter parameter) {
        return new FlagModel(name, shorthand, false, null, parameter);
    }

    /** The long flag name, given on the command line as {@code --name}. */
    String name() {
        return name;
    }

    /** The single-character shorthand given as {@code -x}, or {@code 0} when the flag has none. */
    char shorthand() {
        return shorthand;
    }

    /** Whether this entry expects a following value ({@code true}) or is a bare boolean switch. */
    boolean isValueFlag() {
        return valueFlag;
    }

    /** The resolver that parses a value flag's value; {@code null} for a switch. */
    @Nullable ParamResolver<?> resolver() {
        return resolver;
    }

    /** The reflected method parameter this flag binds to. */
    Parameter parameter() {
        return parameter;
    }
}
