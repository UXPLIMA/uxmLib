package com.uxplima.uxmlib.command.annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.mojang.brigadier.context.CommandContext;
import org.junit.jupiter.api.Test;

/**
 * Verifies that a value flag's raw token is resolved through the same {@link ParamResolver} an
 * {@code @Arg} would use: {@link FlagValues#resolve} builds a throwaway one-argument parse and hands the
 * resulting context back to the resolver. Exercised with the primitive/string resolvers, whose parse never
 * touches the live server, so a mocked source suffices.
 */
class FlagValuesTest {

    @Test
    void resolvesAnIntegerValueFlagThroughItsResolver() {
        ParamResolver<?> intResolver =
                java.util.Objects.requireNonNull(ParamResolvers.withDefaults().resolverFor(int.class));
        Object value = FlagValues.resolve(intResolver, context(), "count", "42");
        assertThat(value).isEqualTo(42);
    }

    @Test
    void resolvesAStringValueFlagThroughItsResolver() {
        ParamResolver<?> stringResolver =
                java.util.Objects.requireNonNull(ParamResolvers.withDefaults().resolverFor(String.class));
        Object value = FlagValues.resolve(stringResolver, context(), "reason", "afk");
        assertThat(value).isEqualTo("afk");
    }

    @Test
    void rejectsAnUnparseableValueWithACleanError() {
        ParamResolver<?> intResolver =
                java.util.Objects.requireNonNull(ParamResolvers.withDefaults().resolverFor(int.class));
        assertThatThrownBy(() -> FlagValues.resolve(intResolver, context(), "count", "notanumber"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @SuppressWarnings("unchecked")
    private static CommandContext<CommandSourceStack> context() {
        CommandSourceStack source = mock(CommandSourceStack.class);
        CommandContext<CommandSourceStack> ctx = mock(CommandContext.class);
        when(ctx.getSource()).thenReturn(source);
        return ctx;
    }
}
