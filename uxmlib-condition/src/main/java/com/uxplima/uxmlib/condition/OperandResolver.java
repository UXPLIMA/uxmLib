package com.uxplima.uxmlib.condition;

import org.bukkit.entity.Player;

import org.jspecify.annotations.Nullable;

/**
 * The seam through which a {@link PlaceholderCondition} turns an operand <em>template</em> (such as
 * {@code "%player_health%"} or a literal {@code "10"}) into a concrete string for the subject of a request.
 *
 * <p>This is a plain function, never a dependency on the integration/PAPI module: a consumer that has
 * PlaceholderAPI present passes {@code PlaceholderAPI::setPlaceholders} (adapted to this shape); a consumer
 * without it passes an identity or its own resolver; tests pass a fake map. Keeping the contract here means
 * the condition module never reaches "upward" into integration.
 *
 * <p>The {@code player} may be {@code null} when a request carries only a generic actor; an implementation
 * that needs a player to resolve a template should return the template unchanged in that case.
 */
@FunctionalInterface
public interface OperandResolver {

    /** Resolve a single operand template against the (optional) subject player. Never returns {@code null}. */
    String resolve(@Nullable Player player, String template);

    /**
     * An identity resolver: every template resolves to itself. Useful for purely literal comparisons and as
     * a test default.
     */
    static OperandResolver identity() {
        return (player, template) -> template;
    }

    /** A resolver that ignores the player entirely, applying a plain string function to the template. */
    static OperandResolver ofTemplate(java.util.function.UnaryOperator<String> function) {
        java.util.Objects.requireNonNull(function, "function");
        return (player, template) -> function.apply(template);
    }

    /** Adapt a {@code (Player, String) -> String} function (e.g. a PlaceholderAPI bridge) to this seam. */
    static OperandResolver ofBiFunction(java.util.function.BiFunction<@Nullable Player, String, String> function) {
        java.util.Objects.requireNonNull(function, "function");
        return (player, template) -> function.apply(player, template);
    }
}
