package com.uxplima.uxmlib.packet.npc;

/**
 * The sixteen named chat colours a glowing NPC can glow in. A glow colour is carried on a scoreboard team the
 * NPC's profile name joins, and the client tints the entity outline with the team's colour. This enum names each
 * colour port-side so the {@link NpcPackets#glowColor} contract stays free of {@code net.minecraft}'s
 * {@code ChatFormatting}; the constant names match {@code ChatFormatting}'s own, so the NMS implementation maps
 * one to the other by name with no lookup table to drift.
 */
public enum NamedColor {
    BLACK,
    DARK_BLUE,
    DARK_GREEN,
    DARK_AQUA,
    DARK_RED,
    DARK_PURPLE,
    GOLD,
    GRAY,
    DARK_GRAY,
    BLUE,
    GREEN,
    AQUA,
    RED,
    LIGHT_PURPLE,
    YELLOW,
    WHITE;
}
