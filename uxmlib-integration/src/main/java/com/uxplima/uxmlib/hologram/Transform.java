package com.uxplima.uxmlib.hologram;

import org.bukkit.util.Transformation;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A hologram's scale and rotation, in friendly terms (a uniform or per-axis scale and a yaw plus pitch in
 * degrees) rather than raw JOML. Maps to the native {@link Transformation} a {@code TextDisplay} accepts, so a
 * hologram can be grown, shrunk, or spun without any packets. The yaw is a rotation about the vertical axis and
 * the pitch a rotation about the horizontal (X) axis; a pure-yaw transform leaves the pitch at zero.
 */
public record Transform(float scaleX, float scaleY, float scaleZ, float yawDegrees, float pitchDegrees) {

    /** No scaling beyond default, no rotation. */
    public static final Transform NONE = new Transform(1f, 1f, 1f, 0f, 0f);

    /** A uniform scale on all axes, no rotation. */
    public static Transform scale(float factor) {
        return new Transform(factor, factor, factor, 0f, 0f);
    }

    /** A rotation of {@code degrees} about the vertical axis, no pitch and no scaling. */
    public static Transform rotation(float degrees) {
        return new Transform(1f, 1f, 1f, degrees, 0f);
    }

    /** A rotation of {@code yaw} about the vertical axis and {@code pitch} about the horizontal axis, no scaling. */
    public static Transform rotation(float yaw, float pitch) {
        return new Transform(1f, 1f, 1f, yaw, pitch);
    }

    /** This transform with a different uniform scale. */
    public Transform withScale(float factor) {
        return new Transform(factor, factor, factor, yawDegrees, pitchDegrees);
    }

    /** This transform with a different yaw rotation in degrees. */
    public Transform withYaw(float degrees) {
        return new Transform(scaleX, scaleY, scaleZ, degrees, pitchDegrees);
    }

    /** This transform with a different pitch rotation in degrees. */
    public Transform withPitch(float degrees) {
        return new Transform(scaleX, scaleY, scaleZ, yawDegrees, degrees);
    }

    /** The native transformation: our scale and a left rotation of yaw about Y then pitch about X. */
    Transformation toBukkit() {
        Quaternionf rotation = new Quaternionf()
                .rotateY((float) Math.toRadians(yawDegrees))
                .rotateX((float) Math.toRadians(pitchDegrees));
        return new Transformation(
                new Vector3f(0f, 0f, 0f), rotation, new Vector3f(scaleX, scaleY, scaleZ), new Quaternionf());
    }
}
