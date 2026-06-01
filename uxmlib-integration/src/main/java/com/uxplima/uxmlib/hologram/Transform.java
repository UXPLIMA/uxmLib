package com.uxplima.uxmlib.hologram;

import org.bukkit.util.Transformation;

import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * A hologram's scale and rotation, in friendly terms (a uniform or per-axis scale and a yaw in degrees)
 * rather than raw JOML. Maps to the native {@link Transformation} a {@code TextDisplay} accepts, so a
 * hologram can be grown, shrunk, or spun without any packets.
 */
public record Transform(float scaleX, float scaleY, float scaleZ, float yawDegrees) {

    /** No scaling beyond default, no rotation. */
    public static final Transform NONE = new Transform(1f, 1f, 1f, 0f);

    /** A uniform scale on all axes, no rotation. */
    public static Transform scale(float factor) {
        return new Transform(factor, factor, factor, 0f);
    }

    /** A rotation of {@code degrees} about the vertical axis, no scaling. */
    public static Transform rotation(float degrees) {
        return new Transform(1f, 1f, 1f, degrees);
    }

    /** This transform with a different uniform scale. */
    public Transform withScale(float factor) {
        return new Transform(factor, factor, factor, yawDegrees);
    }

    /** This transform with a different yaw rotation in degrees. */
    public Transform withYaw(float degrees) {
        return new Transform(scaleX, scaleY, scaleZ, degrees);
    }

    /** The native transformation: our scale and a left rotation about the Y axis. */
    Transformation toBukkit() {
        AxisAngle4f rotation = new AxisAngle4f((float) Math.toRadians(yawDegrees), 0f, 1f, 0f);
        return new Transformation(
                new Vector3f(0f, 0f, 0f), rotation, new Vector3f(scaleX, scaleY, scaleZ), new AxisAngle4f());
    }
}
