package com.uxplima.uxmlib.hologram;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import net.kyori.adventure.text.Component;

import com.uxplima.uxmlib.text.Text;

/**
 * Renders a leaderboard to a list of {@link Component} lines a hologram can show. Pure: it takes the
 * scores, sorts and trims them per {@link LeaderboardOptions}, resolves each UUID to a name through an
 * injected function, and substitutes the {@code {place}}/{@code {name}}/{@code {score}} tokens in the
 * per-place MiniMessage format. No server needed, so it is fully unit-testable.
 */
public final class LeaderboardRenderer {

    private final LeaderboardOptions options;
    private final Function<UUID, String> nameLookup;

    public LeaderboardRenderer(LeaderboardOptions options, Function<UUID, String> nameLookup) {
        this.options = Objects.requireNonNull(options, "options");
        this.nameLookup = Objects.requireNonNull(nameLookup, "nameLookup");
    }

    /** Render {@code scores} into the leaderboard lines, longest header (if any) first. */
    public List<Component> render(Map<UUID, Double> scores) {
        Objects.requireNonNull(scores, "scores");
        List<Map.Entry<UUID, Double>> ranked = rank(scores);
        NumberFormat format = NumberFormat.getInstance(options.locale());
        List<Component> lines = new ArrayList<>();
        options.header().ifPresent(lines::add);
        for (int i = 0; i < options.topN(); i++) {
            lines.add(lineFor(i, ranked, format));
        }
        return lines;
    }

    private List<Map.Entry<UUID, Double>> rank(Map<UUID, Double> scores) {
        List<Map.Entry<UUID, Double>> ranked = new ArrayList<>(scores.entrySet());
        Comparator<Map.Entry<UUID, Double>> byScore = Comparator.comparingDouble(Map.Entry::getValue);
        ranked.sort(options.descending() ? byScore.reversed() : byScore);
        return ranked;
    }

    private Component lineFor(int index, List<Map.Entry<UUID, Double>> ranked, NumberFormat format) {
        if (index >= ranked.size()) {
            return Text.mini(options.emptyLine());
        }
        Map.Entry<UUID, Double> entry = ranked.get(index);
        String template = options.formatForPlace(index + 1);
        String filled = template.replace("{place}", Integer.toString(index + 1))
                .replace("{name}", nameLookup.apply(entry.getKey()))
                .replace("{score}", format.format(entry.getValue()));
        return Text.mini(filled);
    }
}
