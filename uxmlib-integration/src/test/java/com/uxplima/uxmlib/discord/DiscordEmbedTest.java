package com.uxplima.uxmlib.discord;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Pure tests of the embed JSON encoding — no network, no Bukkit. */
class DiscordEmbedTest {

    @Test
    void encodesTitleAndDescription() {
        String body = DiscordWebhook.embedBody(DiscordEmbed.of("Title", "Body"));
        assertThat(body).isEqualTo("{\"embeds\":[{\"title\":\"Title\",\"description\":\"Body\"}]}");
    }

    @Test
    void includesColourWhenSet() {
        // 0xFF8800 == 16746496 decimal — Discord embed colours are the decimal RGB integer.
        String body = DiscordWebhook.embedBody(DiscordEmbed.colored("T", "D", 0xFF8800));
        assertThat(body).isEqualTo("{\"embeds\":[{\"title\":\"T\",\"description\":\"D\",\"color\":16746496}]}");
    }

    @Test
    void escapesSpecialCharactersInFields() {
        String body = DiscordWebhook.embedBody(DiscordEmbed.of("a\"b", "line1\nline2"));
        assertThat(body).contains("\"title\":\"a\\\"b\"").contains("\"description\":\"line1\\nline2\"");
    }

    @Test
    void escapesControlCharactersAsUnicode() {
        // A bare 0x01 is illegal unescaped in JSON and must become \\u0001; backspace (0x08) and form-feed
        // (0x0C) get their short escapes \\b and \\f. Build the control chars explicitly to be unambiguous.
        String title = "a" + (char) 0x01 + "b";
        String description = "c" + (char) 0x08 + "d" + (char) 0x0C + "e";
        String body = DiscordWebhook.embedBody(DiscordEmbed.of(title, description));
        assertThat(body).contains("\"title\":\"a\\u0001b\"").contains("\"description\":\"c\\bd\\fe\"");
    }

    @Test
    void colorValueReflectsPresence() {
        assertThat(DiscordEmbed.of("t", "d").colorValue()).isEmpty();
        assertThat(DiscordEmbed.colored("t", "d", 1).colorValue()).contains(1);
    }
}
