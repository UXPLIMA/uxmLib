package com.uxplima.uxmlib.command.annotation;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContext;
import com.uxplima.uxmlib.command.Cmd;
import com.uxplima.uxmlib.command.annotation.annotations.Arg;

/**
 * Parses a value flag's raw token through the same {@link ParamResolver} an {@code @}{@link Arg} would use,
 * so a {@code @}{@link com.uxplima.uxmlib.command.annotation.annotations.Flag} {@code Player p} resolves a
 * player exactly like {@code @Arg Player p}. A resolver reads its value out of a {@link CommandContext} by
 * argument name; a flag token is not a node in the dispatched tree, so here we parse the token through a
 * throwaway one-argument Brigadier tree built from the resolver's own argument type and hand the resulting
 * context back to the resolver. This keeps flag values type-uniform with positional arguments without the
 * resolver knowing it came from a flag. A parse failure surfaces as an {@link IllegalArgumentException} on
 * the clean-error path a bad argument uses.
 */
final class FlagValues {

    private FlagValues() {}

    /** Resolve {@code raw} as the value for flag {@code name} using {@code resolver} and the live source. */
    static Object resolve(
            ParamResolver<?> resolver, CommandContext<CommandSourceStack> outer, String name, String raw) {
        CommandContext<CommandSourceStack> parsed = parse(resolver, outer.getSource(), name, raw);
        Object value = resolver.resolve(parsed, name);
        if (value == null) {
            throw new IllegalArgumentException("Invalid value for --" + name + ": " + raw);
        }
        return value;
    }

    private static CommandContext<CommandSourceStack> parse(
            ParamResolver<?> resolver, CommandSourceStack source, String name, String raw) {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.register(Cmd.literal("flag").then(Cmd.argument(name, argumentType(resolver))));
        ParseResults<CommandSourceStack> results = dispatcher.parse("flag " + raw, source);
        if (!results.getExceptions().isEmpty() || results.getReader().canRead()) {
            throw new IllegalArgumentException("Invalid value for --" + name + ": " + raw);
        }
        return results.getContext().build("flag " + raw);
    }

    /** The native argument type for a flag value: a plain (non-greedy) read, since a flag value is one token. */
    private static com.mojang.brigadier.arguments.ArgumentType<?> argumentType(ParamResolver<?> resolver) {
        return resolver.argumentType(DEFAULT_ARG);
    }

    private static final Arg DEFAULT_ARG = defaultArg();

    /**
     * A default {@code @Arg} (all fields at their defaults: no bounds, not greedy, not optional) handed to
     * {@link ParamResolver#argumentType(Arg)} when building a flag value's throwaway parse tree, since a flag
     * value is always a single non-greedy token. Read off the {@code @Arg}-annotated parameter of the private
     * {@link #defaultArgHolder(Object)} method rather than hand-implemented, so it is a real annotation proxy
     * with the equals/hashCode the platform expects of an {@link java.lang.annotation.Annotation}.
     */
    private static Arg defaultArg() {
        try {
            Arg arg = FlagValues.class
                    .getDeclaredMethod("defaultArgHolder", Object.class)
                    .getParameters()[0]
                    .getAnnotation(Arg.class);
            if (arg == null) {
                throw new IllegalStateException("defaultArgHolder lost its @Arg annotation");
            }
            return arg;
        } catch (NoSuchMethodException impossible) {
            throw new IllegalStateException("defaultArgHolder method is missing", impossible);
        }
    }

    @SuppressWarnings("unused") // only its parameter's @Arg annotation is read reflectively, never called
    private static void defaultArgHolder(@Arg("") Object placeholder) {}
}
