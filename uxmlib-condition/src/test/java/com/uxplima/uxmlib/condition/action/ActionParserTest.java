package com.uxplima.uxmlib.condition.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * The parser is pure: a config string maps to the right {@link ActionType}, the payload after the prefix, and
 * a closure. These tests assert the routing and the documented error cases without running any side effect.
 */
class ActionParserTest {

    @Test
    void messageRoutesToMessageTypeAndKeepsThePayloadVerbatim() {
        ParsedAction parsed = ActionParser.parse("[message] <green>Hello, %player_name%!");
        assertThat(parsed.type()).isEqualTo(ActionType.MESSAGE);
        assertThat(parsed.payload()).isEqualTo("<green>Hello, %player_name%!");
    }

    @Test
    void prefixMatchingIsCaseInsensitive() {
        assertThat(ActionParser.parse("[BROADCAST] hi").type()).isEqualTo(ActionType.BROADCAST);
        assertThat(ActionParser.parse("[Console] say hi").type()).isEqualTo(ActionType.CONSOLE);
    }

    @Test
    void everyKnownPrefixParses() {
        assertThat(ActionParser.parse("[player] spawn").type()).isEqualTo(ActionType.PLAYER);
        assertThat(ActionParser.parse("[actionbar] hi").type()).isEqualTo(ActionType.ACTIONBAR);
        assertThat(ActionParser.parse("[title] hi").type()).isEqualTo(ActionType.TITLE);
        assertThat(ActionParser.parse("[sound] minecraft:ui.button.click").type())
                .isEqualTo(ActionType.SOUND);
    }

    @Test
    void closeNeedsNoPayload() {
        ParsedAction parsed = ActionParser.parse("[close]");
        assertThat(parsed.type()).isEqualTo(ActionType.CLOSE);
        assertThat(parsed.payload()).isEmpty();
    }

    @Test
    void leadingWhitespaceBeforeThePrefixIsTolerated() {
        assertThat(ActionParser.parse("   [message] hi").type()).isEqualTo(ActionType.MESSAGE);
    }

    @Test
    void unknownPrefixIsRejectedWithTheName() {
        assertThatThrownBy(() -> ActionParser.parse("[teleport] world"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("teleport");
    }

    @Test
    void aStringWithoutABracketPrefixIsRejected() {
        assertThatThrownBy(() -> ActionParser.parse("message hi"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("[prefix]");
    }

    @Test
    void aMissingClosingBracketIsRejected() {
        assertThatThrownBy(() -> ActionParser.parse("[message hi"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("closing");
    }

    @Test
    void aPayloadRequiringTypeWithoutAPayloadIsRejected() {
        assertThatThrownBy(() -> ActionParser.parse("[message]"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("needs a payload");
    }

    @Test
    void aNonNumericSoundVolumeIsRejectedAtParseTime() {
        assertThatThrownBy(() -> ActionParser.parse("[sound] minecraft:ui.button.click loud"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("volume");
    }

    @Test
    void textActionsAreFlaggedAsyncWhileCommandActionsAreSync() {
        assertThat(ActionParser.parse("[message] hi").action().async()).isTrue();
        assertThat(ActionParser.parse("[actionbar] hi").action().async()).isTrue();
        assertThat(ActionParser.parse("[console] say hi").action().async()).isFalse();
        assertThat(ActionParser.parse("[player] spawn").action().async()).isFalse();
        assertThat(ActionParser.parse("[close]").action().async()).isFalse();
    }
}
