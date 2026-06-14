package com.uxplima.uxmlib.packet.tablist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

/** Value-semantics of the {@link TabSkin} record: construction, the unsigned factory, and the value guard. */
class TabSkinTest {

    @Test
    void carriesValueAndSignature() {
        TabSkin skin = new TabSkin("base64value", "sig");

        assertThat(skin.textureValue()).isEqualTo("base64value");
        assertThat(skin.signature()).isEqualTo("sig");
    }

    @Test
    void unsignedHasNoSignature() {
        TabSkin skin = TabSkin.unsigned("base64value");

        assertThat(skin.textureValue()).isEqualTo("base64value");
        assertThat(skin.signature()).isNull();
    }

    @Test
    void rejectsNullTextureValue() {
        assertThatNullPointerException().isThrownBy(() -> new TabSkin(nullString(), "sig"));
    }

    @SuppressWarnings("NullAway") // intentionally feeds null to assert the constructor guard fires.
    private static String nullString() {
        return null;
    }
}
