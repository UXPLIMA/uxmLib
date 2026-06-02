package com.uxplima.uxmlib.command.annotation.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code boolean} handler parameter as a <em>switch</em>: a position-independent presence flag given
 * on the command line as {@code --name} (or {@code -x} via the {@link #shorthand()}). The parameter is
 * {@code true} when the switch is present and {@code false} when it is absent; a switch takes no following
 * value. Like a {@code @}{@link Flag}, switches may appear in any order after the positional {@code @Arg}s
 * and are validated to come last at registration. The parameter type must be {@code boolean} or
 * {@link Boolean}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Switch {

    /** The long switch name, given as {@code --name} (without the leading dashes). */
    String value();

    /** A single-character shorthand given as {@code -x}, or {@code 0} (the default) for no shorthand. */
    char shorthand() default 0;
}
