package com.uxplima.uxmlib.packet.npc;

/**
 * The six posable parts of an armor stand. Each maps to one of the stand's {@code DATA_*_POSE} metadata items,
 * which carry a set of Euler angles; naming them port-side keeps the {@link NpcPackets#armorStandPose} contract
 * free of {@code net.minecraft}. The NMS implementation resolves each constant to its matching pose accessor.
 */
public enum ArmorStandPart {
    HEAD,
    BODY,
    LEFT_ARM,
    RIGHT_ARM,
    LEFT_LEG,
    RIGHT_LEG
}
