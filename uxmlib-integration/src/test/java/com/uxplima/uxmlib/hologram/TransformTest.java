package com.uxplima.uxmlib.hologram;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Pure tests of the friendly Transform -> native Transformation mapping. */
class TransformTest {

    @Test
    void uniformScaleSetsAllAxes() {
        Transform t = Transform.scale(2.0f);
        assertThat(t.scaleX()).isEqualTo(2.0f);
        assertThat(t.scaleY()).isEqualTo(2.0f);
        assertThat(t.scaleZ()).isEqualTo(2.0f);
        assertThat(t.yawDegrees()).isZero();
    }

    @Test
    void rotationKeepsDefaultScale() {
        Transform t = Transform.rotation(90f);
        assertThat(t.scaleX()).isEqualTo(1.0f);
        assertThat(t.yawDegrees()).isEqualTo(90f);
    }

    @Test
    void withScaleAndWithYawCompose() {
        Transform t = Transform.NONE.withScale(3f).withYaw(45f);
        assertThat(t.scaleX()).isEqualTo(3f);
        assertThat(t.yawDegrees()).isEqualTo(45f);
    }

    @Test
    void mapsToNativeTransformationScale() {
        var bukkit = Transform.scale(2.5f).toBukkit();
        assertThat(bukkit.getScale().x()).isEqualTo(2.5f);
        assertThat(bukkit.getScale().y()).isEqualTo(2.5f);
    }

    @Test
    void rotationWithPitchKeepsDefaultScaleAndBothAngles() {
        Transform t = Transform.rotation(90f, 30f);
        assertThat(t.scaleX()).isEqualTo(1.0f);
        assertThat(t.yawDegrees()).isEqualTo(90f);
        assertThat(t.pitchDegrees()).isEqualTo(30f);
    }

    @Test
    void noneAndYawOnlyRotationCarryZeroPitch() {
        assertThat(Transform.NONE.pitchDegrees()).isZero();
        assertThat(Transform.rotation(45f).pitchDegrees()).isZero();
    }

    @Test
    void withPitchKeepsTheOtherFields() {
        Transform t = Transform.NONE.withScale(2f).withYaw(15f).withPitch(40f);
        assertThat(t.scaleX()).isEqualTo(2f);
        assertThat(t.yawDegrees()).isEqualTo(15f);
        assertThat(t.pitchDegrees()).isEqualTo(40f);
    }

    @Test
    void aPitchedTransformStillMapsItsScale() {
        var bukkit = Transform.rotation(20f, 50f).withScale(1.5f).toBukkit();
        assertThat(bukkit.getScale().x()).isEqualTo(1.5f);
        assertThat(bukkit.getScale().y()).isEqualTo(1.5f);
        assertThat(bukkit.getScale().z()).isEqualTo(1.5f);
    }
}
