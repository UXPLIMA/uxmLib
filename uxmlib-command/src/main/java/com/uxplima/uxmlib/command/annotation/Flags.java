package com.uxplima.uxmlib.command.annotation;

import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The parsed result of a branch's trailing flag argument: the raw token of every value flag that was given,
 * and the set of switches that were present. {@link FlagArgumentType} produces it; {@link ArgBinder} reads
 * it to fill each {@code @}{@link com.uxplima.uxmlib.command.annotation.annotations.Flag} /
 * {@code @}{@link com.uxplima.uxmlib.command.annotation.annotations.Switch} parameter. Value flags are kept
 * as their raw string so the binder can hand them to the same {@link ParamResolver} an {@code @Arg} would
 * use, keeping the type story uniform.
 */
final class Flags {

    private final Map<String, String> values;
    private final Map<String, Boolean> switches;

    Flags(Map<String, String> values, Map<String, Boolean> switches) {
        this.values = Map.copyOf(Objects.requireNonNull(values, "values"));
        this.switches = Map.copyOf(Objects.requireNonNull(switches, "switches"));
    }

    /** The raw token given for value flag {@code name}, or {@code null} when the flag was not provided. */
    @Nullable String value(String name) {
        return values.get(name);
    }

    /** Whether switch {@code name} was present. */
    boolean isSet(String name) {
        return Boolean.TRUE.equals(switches.get(name));
    }
}
