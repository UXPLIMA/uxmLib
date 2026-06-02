package com.uxplima.uxmlib.command.annotation.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Hides a command branch from the auto-generated {@code /help} listing. A {@link Subcommand} method (or a
 * whole {@link Command} class) marked secret still runs for a sender who types it and holds any required
 * {@link Permission}; it is simply omitted from the generated help so it is not advertised to everyone.
 *
 * <p>Secrecy here is a help-listing concern, not a security boundary: gate a command you truly want hidden
 * with a {@link Permission} so Brigadier's own {@code requires} keeps it out of tab-completion as well. The
 * intent of {@code @Secret} is the "easter egg" / staff-only-but-unguarded case where you only want the
 * command kept off the public help page.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Secret {}
