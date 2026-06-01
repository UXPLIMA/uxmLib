package com.uxplima.uxmlib.command.annotation;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

/**
 * A context-aware tab-completion provider, mirroring Brigadier's suggestion model: it sees the partially
 * typed command (including earlier already-parsed arguments via the context) and returns the completions.
 * Because the result is a {@link java.util.concurrent.CompletableFuture}, a provider may compute
 * off-thread and compose natively. Reference one from {@code @}{@link SuggestWith}; the provider class
 * must have a public no-argument constructor.
 */
@FunctionalInterface
public interface SuggestionSource {

    /** Offer completions for the current {@code builder} state. */
    java.util.concurrent.CompletableFuture<Suggestions> suggest(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder);
}
