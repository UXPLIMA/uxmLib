package com.uxplima.uxmlib.hologram;

import org.bukkit.util.Transformation;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A hologram's display transform, in friendly terms: a translation offset (x, y, z in display space), a
 * uniform or per-axis scale, and a yaw plus pitch in degrees, rather than raw JOML. Maps to the native
 * {@link Transformation} a {@code Display} accepts, so a hologram can be nudged, grown, shrunk, or spun without
 * any packets. The yaw is a rotation about the vertical axis and the pitch a rotation about the horizontal (X)
 * axis; a pure-yaw transform leaves the pitch at zero. The translation shifts the display relative to its
 * anchor location, the FancyHolograms-style positional offset.
 */
public record Transform(
        float transX,
        float transY,
        float transZ,
        float scaleX,
        float scaleY,
        float scaleZ,
        float yawDegrees,
        float pitchDegrees) {

    /** No translation, no scaling beyond default, no rotation. */
    public static final Transform NONE = new Transform(0f, 0f, 0f, 1f, 1f, 1f, 0f, 0f);

    /** A uniform scale on all axes, no translation and no rotation. */
    public static Transform scale(float factor) {
        return new Transform(0f, 0f, 0f, factor, factor, factor, 0f, 0f);
    }

    /** A per-axis scale, no translation and no rotation. */
    public static Transform scale(float x, float y, float z) {
        return new Transform(0f, 0f, 0f, x, y, z, 0f, 0f);
    }

    /** A translation offset of {@code (x, y, z)} in display space, no scaling and no rotation. */
    public static Transform translation(float x, float y, float z) {
        return new Transform(x, y, z, 1f, 1f, 1f, 0f, 0f);
    }

    /** A rotation of {@code degrees} about the vertical axis, no translation, pitch, or scaling. */
    public static Transform rotation(float degrees) {
        return new Transform(0f, 0f, 0f, 1f, 1f, 1f, degrees, 0f);
    }

    /** A rotation of {@code yaw} about the vertical axis and {@code pitch} about the horizontal axis, no scaling. */
    public static Transform rotation(float yaw, float pitch) {
        return new Transform(0f, 0f, 0f, 1f, 1f, 1f, yaw, pitch);
    }

    /** This transform with a different uniform scale. */
    public Transform withScale(float factor) {
        return new Transform(transX, transY, transZ, factor, factor, factor, yawDegrees, pitchDegrees);
    }

    /** This transform with a different per-axis scale. */
    public Transform withScale(float x, float y, float z) {
        return new Transform(transX, transY, transZ, x, y, z, yawDegrees, pitchDegrees);
    }

    /** This transform with a different translation offset. */
    public Transform withTranslation(float x, float y, float z) {
        return new Transform(x, y, z, scaleX, scaleY, scaleZ, yawDegrees, pitchDegrees);
    }

    /** This transform with a different yaw rotation in degrees. */
    public Transform withYaw(float degrees) {
        return new Transform(transX, transY, transZ, scaleX, scaleY, scaleZ, degrees, pitchDegrees);
    }

    /** This transform with a different pitch rotation in degrees. */
    public Transform withPitch(float degrees) {
        return new Transform(transX, transY, transZ, scaleX, scaleY, scaleZ, yawDegrees, degrees);
    }

    /** The native transformation: our translation and scale and a left rotation of yaw about Y then pitch about X. */
    Transformation toBukkit() {
        Quaternionf rotation = new Quaternionf()
                .rotateY((float) Math.toRadians(yawDegrees))
                .rotateX((float) Math.toRadians(pitchDegrees));
        return new Transformation(
                new Vector3f(transX, transY, transZ),
                rotation,
                new Vector3f(scaleX, scaleY, scaleZ),
                new Quaternionf());
    }
}
