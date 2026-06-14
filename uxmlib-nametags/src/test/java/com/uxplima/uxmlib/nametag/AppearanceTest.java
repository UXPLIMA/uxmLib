package com.uxplima.uxmlib.nametag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/** Value-semantics of the {@link Appearance} record: the documented defaults, immutability, and guards. */
class AppearanceTest {

    @Test
    void defaultsMatchTheDocumentedOutOfBoxLook() {
        Appearance a = Appearance.defaults();
        assertThat(a.billboard()).isEqualTo(Billboard.CENTER);
        assertThat(a.backgroundArgb()).isZero();
        assertThat(a.textShadow()).isFalse();
        assertThat(a.seeThrough()).isFalse();
        assertThat(a.alignment()).isEqualTo(Alignment.CENTER);
        assertThat(a.lineWidth()).isEqualTo(200);
        assertThat(a.viewRange()).isEqualTo(1.0f);
        assertThat(a.translation()).isEqualTo(new Vector3f(0f, 0f, 0f));
        assertThat(a.scale()).isEqualTo(new Vector3f(1f, 1f, 1f));
        assertThat(a.interpolationDurationTicks()).isZero();
        assertThat(a.hideThroughBlocks()).isFalse();
        assertThat(a.obscuredOpacity()).isEqualTo(64);
    }

    @Test
    void translationAndScaleAreDefensivelyCopied() {
        Vector3f translation = new Vector3f(1f, 2f, 3f);
        Vector3f scale = new Vector3f(4f, 5f, 6f);
        Appearance a = new Appearance(
                Billboard.FIXED, 0, false, false, Alignment.LEFT, 10, 0.5f, translation, scale, 0, false, 64);

        translation.set(99f, 99f, 99f);
        scale.set(99f, 99f, 99f);

        assertThat(a.translation()).isEqualTo(new Vector3f(1f, 2f, 3f));
        assertThat(a.scale()).isEqualTo(new Vector3f(4f, 5f, 6f));
    }

    @Test
    void rejectsObscuredOpacityOutOfRange() {
        assertThatIllegalArgumentException().isThrownBy(() -> withObscuredOpacity(-1));
        assertThatIllegalArgumentException().isThrownBy(() -> withObscuredOpacity(256));
    }

    private static Appearance withObscuredOpacity(int opacity) {
        return new Appearance(
                Billboard.CENTER,
                0,
                false,
                false,
                Alignment.CENTER,
                200,
                1.0f,
                new Vector3f(),
                new Vector3f(1f, 1f, 1f),
                0,
                false,
                opacity);
    }
}
