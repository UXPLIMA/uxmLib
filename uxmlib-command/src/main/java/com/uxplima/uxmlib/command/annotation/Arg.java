package com.uxplima.uxmlib.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Names a command argument parameter. The parameter's Java type drives the Brigadier argument type
 * (String, int, double, boolean are built in); the {@link #value()} is the argument name shown in usage
 * and used to read it back. Optional numeric bounds apply to {@code int}/{@code double} parameters.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Arg {

    /** The argument name (e.g. {@code "amount"}). */
    String value();

    /** Inclusive minimum for a numeric argument; ignored for non-numeric types. */
    double min() default Double.NEGATIVE_INFINITY;

    /** Inclusive maximum for a numeric argument; ignored for non-numeric types. */
    double max() default Double.POSITIVE_INFINITY;
}
