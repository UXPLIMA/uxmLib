package com.uxplima.uxmlib.command.annotation;

import java.lang.reflect.Parameter;
import java.util.Collection;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

/**
 * Wires tab-completion onto an argument node, choosing the source by precedence: an explicit
 * {@code @}{@link SuggestWith} provider, then a static {@code @}{@link Suggest} list, then the resolver's
 * own {@link ParamResolver#suggestions()} (e.g. an enum's constants). When none applies the node keeps
 * the argument type's native suggestions (a player or world arg completes itself), so this only attaches a
 * provider when there is something to override with.
 */
final class Suggestions {

    private Suggestions() {}

    static void apply(
            RequiredArgumentBuilder<CommandSourceStack, ?> builder, Parameter parameter, ParamResolver<?> resolver) {
        SuggestWith suggestWith = parameter.getAnnotation(SuggestWith.class);
        if (suggestWith != null) {
            builder.suggests(fromProvider(instantiate(suggestWith.value())));
            return;
        }
        Suggest suggest = parameter.getAnnotation(Suggest.class);
        if (suggest != null) {
            builder.suggests(fromList(java.util.List.of(suggest.value())));
            return;
        }
        Collection<String> fromResolver = resolver.suggestions();
        if (fromResolver != null) {
            builder.suggests(fromList(fromResolver));
        }
    }

    private static SuggestionSource instantiate(Class<? extends SuggestionSource> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException failure) {
            throw new CommandParseException(
                    "@SuggestWith provider " + type.getName() + " needs a public no-arg constructor", failure);
        }
    }

    private static SuggestionProvider<CommandSourceStack> fromProvider(SuggestionSource source) {
        return source::suggest;
    }

    /** A provider that offers each value whose lower-case form starts with what the player has typed. */
    private static SuggestionProvider<CommandSourceStack> fromList(Collection<String> values) {
        return (CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) -> {
            String remaining = builder.getRemainingLowerCase();
            for (String value : values) {
                if (value.toLowerCase(java.util.Locale.ROOT).startsWith(remaining)) {
                    builder.suggest(value);
                }
            }
            return builder.buildFuture();
        };
    }
}
