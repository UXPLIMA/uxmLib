package com.uxplima.uxmlib.nametag;

/**
 * How a nametag faces the viewer, mirroring vanilla's {@code Display.BillboardConstraints}. {@link #CENTER}
 * pivots on both axes so the text always faces the camera; the others lock one or both axes.
 */
public enum Billboard {

    /** Always faces the viewer, pivoting on both the vertical and horizontal axes. */
    CENTER,

    /** Never rotates; keeps the orientation it was spawned with. */
    FIXED,

    /** Pivots only about the vertical axis. */
    VERTICAL,

    /** Pivots only about the horizontal axis. */
    HORIZONTAL
}
