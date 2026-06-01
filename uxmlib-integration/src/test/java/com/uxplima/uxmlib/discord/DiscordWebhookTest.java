package com.uxplima.uxmlib.discord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Pure tests of the webhook JSON encoding and URL validation — no network, no Bukkit. */
class DiscordWebhookTest {

    @Test
    void wrapsContentInAJsonObject() {
        assertThat(DiscordWebhook.contentBody("hello")).isEqualTo("{\"content\":\"hello\"}");
    }

    @Test
    void acceptsAWellFormedHttpsUrl() {
        assertThatCode(() -> new DiscordWebhook("https://discord.com/api/webhooks/1/abc"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsAMalformedUrlAtConstruction() {
        assertThatThrownBy(() -> new DiscordWebhook("not a url")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DiscordWebhook("ftp://example.com/x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void escapesJsonSpecialCharacters() {
        assertThat(DiscordWebhook.jsonString("a\"b\\c")).isEqualTo("\"a\\\"b\\\\c\"");
        assertThat(DiscordWebhook.jsonString("line1\nline2")).isEqualTo("\"line1\\nline2\"");
        assertThat(DiscordWebhook.jsonString("tab\tend")).isEqualTo("\"tab\\tend\"");
    }

    @Test
    void leavesPlainTextUntouched() {
        assertThat(DiscordWebhook.jsonString("plain text 123")).isEqualTo("\"plain text 123\"");
    }
}
