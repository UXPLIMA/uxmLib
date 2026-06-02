package com.uxplima.uxmlib.text;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import net.kyori.adventure.text.Component;

import org.junit.jupiter.api.Test;

class PlaceholdersTest {

    private record Profile(String name, int balance) {}

    @Test
    void resolvesNamedPlaceholdersAgainstTheSubject() {
        Placeholders<Profile> placeholders = Placeholders.<Profile>builder()
                .add("name", Profile::name)
                .add("balance", p -> Integer.toString(p.balance()))
                .build();

        Component out = Text.mini("<name> has <balance>", placeholders.resolver(new Profile("Steve", 42)));

        assertThat(Text.plain(out)).isEqualTo("Steve has 42");
    }

    @Test
    void evaluatesEachSupplierLazilyOnlyWhenItsTagIsRendered() {
        AtomicInteger nameCalls = new AtomicInteger();
        AtomicInteger costlyCalls = new AtomicInteger();
        Placeholders<Profile> placeholders = Placeholders.<Profile>builder()
                .add("name", p -> {
                    nameCalls.incrementAndGet();
                    return p.name();
                })
                .add("costly", p -> {
                    costlyCalls.incrementAndGet();
                    return "expensive";
                })
                .build();
        Profile subject = new Profile("Steve", 7);

        // Building the resolver must not touch either function.
        var resolver = placeholders.resolver(subject);
        assertThat(nameCalls).hasValue(0);
        assertThat(costlyCalls).hasValue(0);

        // Rendering a template that uses only <name> evaluates that function (MiniMessage may query a
        // resolver more than once per render, so assert it ran at all) and never touches the unused one.
        Component out = Text.mini("Hi <name>", resolver);
        assertThat(Text.plain(out)).isEqualTo("Hi Steve");
        assertThat(nameCalls).hasValueGreaterThanOrEqualTo(1);
        assertThat(costlyCalls).hasValue(0);
    }

    @Test
    void unparsedPlaceholderShowsValueMarkupLiterally() {
        Placeholders<Profile> placeholders =
                Placeholders.<Profile>builder().add("name", Profile::name).build();

        Component out = Text.mini("<name>", placeholders.resolver(new Profile("<bold>Steve", 0)));

        assertThat(Text.plain(out)).isEqualTo("<bold>Steve");
    }

    @Test
    void parsedPlaceholderRendersValueMarkup() {
        Placeholders<Profile> placeholders =
                Placeholders.<Profile>builder().addParsed("name", Profile::name).build();

        Component out = Text.mini("<name>", placeholders.resolver(new Profile("<red>Steve", 0)));

        // The value's own <red> tag is parsed and stripped, leaving plain "Steve".
        assertThat(Text.plain(out)).isEqualTo("Steve");
    }

    @Test
    void componentPlaceholderInsertsAPrebuiltComponent() {
        Placeholders<Profile> placeholders = Placeholders.<Profile>builder()
                .addComponent("who", p -> Component.text(p.name()))
                .build();

        Component out = Text.mini("Welcome <who>", placeholders.resolver(new Profile("Alice", 0)));

        assertThat(Text.plain(out)).isEqualTo("Welcome Alice");
    }

    @Test
    void renderComposesWithExtraResolvers() {
        Placeholders<Profile> placeholders =
                Placeholders.<Profile>builder().add("name", Profile::name).build();

        Component out =
                placeholders.render("<name> <city>", new Profile("Steve", 0), Text.placeholder("city", "Athens"));

        assertThat(Text.plain(out)).isEqualTo("Steve Athens");
    }

    @Test
    void builderSnapshotIsIndependentOfLaterAdditions() {
        Placeholders.Builder<Profile> builder = Placeholders.<Profile>builder().add("name", Profile::name);
        Placeholders<Profile> first = builder.build();
        builder.add("extra", p -> "later");

        Component out = first.render("<name> <extra>", new Profile("Steve", 0));

        // <extra> was added after the snapshot, so it stays an unresolved literal tag.
        assertThat(Text.plain(out)).isEqualTo("Steve <extra>");
    }
}
