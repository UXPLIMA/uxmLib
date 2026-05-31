package com.uxplima.uxmlib.text;

import static org.assertj.core.api.Assertions.assertThat;

import net.kyori.adventure.text.Component;

import org.junit.jupiter.api.Test;

class TextTest {

    @Test
    void parsesMiniMessageAndFlattensToPlain() {
        Component parsed = Text.mini("<red>Hello</red>");
        assertThat(Text.plain(parsed)).isEqualTo("Hello");
    }

    @Test
    void resolvesAnUnparsedPlaceholderLiterally() {
        Component parsed = Text.mini("Hi <name>!", Text.placeholder("name", "<bold>Steve"));
        // unparsed: the value's tags are shown as text, never parsed.
        assertThat(Text.plain(parsed)).isEqualTo("Hi <bold>Steve!");
    }

    @Test
    void resolvesAComponentPlaceholder() {
        Component parsed = Text.mini("Welcome <who>", Text.component("who", Component.text("Alice")));
        assertThat(Text.plain(parsed)).isEqualTo("Welcome Alice");
    }

    @Test
    void stripsTags() {
        assertThat(Text.stripTags("<green>hello <bold>world</bold></green>")).isEqualTo("hello world");
    }
}
