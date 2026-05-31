package com.uxplima.uxmlib.command.annotation;

import java.util.Map;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

/**
 * Maps a supported Java parameter type to its Brigadier {@link ArgumentType} and the reader that pulls
 * the parsed value back out of a {@link CommandContext}. Keeps the reflective registrar free of a
 * per-type {@code if/else} ladder.
 */
final class ArgType {

    private ArgType() {}

    /** Whether {@code type} can be used as an {@link Arg} parameter. */
    static boolean isSupported(Class<?> type) {
        return type == String.class
                || type == int.class
                || type == Integer.class
                || type == double.class
                || type == Double.class
                || type == boolean.class
                || type == Boolean.class;
    }

    /** The Brigadier argument type for {@code type}, honouring numeric bounds from {@code arg}. */
    static ArgumentType<?> argumentType(Class<?> type, Arg arg) {
        if (type == String.class) {
            return StringArgumentType.word();
        }
        if (type == int.class || type == Integer.class) {
            int min = arg.min() == Double.NEGATIVE_INFINITY ? Integer.MIN_VALUE : (int) arg.min();
            int max = arg.max() == Double.POSITIVE_INFINITY ? Integer.MAX_VALUE : (int) arg.max();
            return IntegerArgumentType.integer(min, max);
        }
        if (type == double.class || type == Double.class) {
            double min = arg.min();
            double max = arg.max();
            return DoubleArgumentType.doubleArg(min, max);
        }
        if (type == boolean.class || type == Boolean.class) {
            return BoolArgumentType.bool();
        }
        throw new CommandParseException("unsupported argument type: " + type.getName());
    }

    /** Read the parsed value for {@code name} out of {@code ctx} as {@code type}. */
    static Object read(CommandContext<CommandSourceStack> ctx, String name, Class<?> type) {
        if (type == String.class) {
            return StringArgumentType.getString(ctx, name);
        }
        if (type == int.class || type == Integer.class) {
            return IntegerArgumentType.getInteger(ctx, name);
        }
        if (type == double.class || type == Double.class) {
            return DoubleArgumentType.getDouble(ctx, name);
        }
        if (type == boolean.class || type == Boolean.class) {
            return BoolArgumentType.getBool(ctx, name);
        }
        throw new CommandParseException("unsupported argument type: " + type.getName());
    }

    /** Boxed types for the primitive forms, so reflective invoke() arguments line up. */
    static final Map<Class<?>, Class<?>> BOXED = Map.of(
            int.class, Integer.class,
            double.class, Double.class,
            boolean.class, Boolean.class);
}
