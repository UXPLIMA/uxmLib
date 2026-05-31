/**
 * Annotation-driven commands. Annotate a handler class with {@link com.uxplima.uxmlib.command.annotation.Command},
 * its methods with {@link com.uxplima.uxmlib.command.annotation.Subcommand}, parameters with
 * {@link com.uxplima.uxmlib.command.annotation.Arg}, and optionally
 * {@link com.uxplima.uxmlib.command.annotation.Permission}; then
 * {@link com.uxplima.uxmlib.command.annotation.AnnotatedCommands#register} reflects over it and builds
 * the Brigadier tree for you — no hand-wiring of nodes. The underlying {@code Cmd}/{@code Sender} facade
 * stays available for cases the annotations do not cover.
 */
@NullMarked
package com.uxplima.uxmlib.command.annotation;

import org.jspecify.annotations.NullMarked;
