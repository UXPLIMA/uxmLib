package com.uxplima.uxmlib.packet.tablist.internal;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.uxplima.uxmlib.npc.PacketSender;
import com.uxplima.uxmlib.packet.Components;
import com.uxplima.uxmlib.packet.tablist.TabEntry;
import com.uxplima.uxmlib.packet.tablist.TabListPackets;
import com.uxplima.uxmlib.packet.tablist.TabSkin;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry;
import net.minecraft.world.level.GameType;
import org.jspecify.annotations.Nullable;

/**
 * The sole NMS-bearing class of the tab-list layer: it builds the real Mojang-mapped player-info packets that
 * paint a per-viewer tab row and writes them through the connection. Quarantining {@code net.minecraft} to one
 * class follows the same precedent as {@code uxmlib-npc}'s {@code ChannelResolver} and the nametag renderer's
 * {@code NmsNametagPackets}, which isolate the unavoidable server-internal reach so the rest of the module
 * stays pure and unit-testable against a fake.
 *
 * <p>Built against the Mojang-mapped 1.21.11 dev bundle; Paper's runtime remapper maps these back to the
 * server's own mappings at load. The update packet is assembled through Paper's public
 * {@code ClientboundPlayerInfoUpdatePacket(EnumSet<Action>, List<Entry>)} constructor (added by
 * Paper's "Add Listing API for Player" patch) — vanilla alone exposes only an {@code Entry}-from-{@code
 * ServerPlayer} path, so this public object constructor is the clean way to seat synthetic entries without the
 * stream-codec round-trip the {@code Codecs} helper is reserved for. The {@code Entry} record's nine
 * components are written in their declared order; the actions {@code EnumSet} tells the client which of them to
 * read, so unused components are filled with harmless defaults.
 */
public final class NmsTabListPackets implements TabListPackets {

    private final PacketSender sender;

    public NmsTabListPackets(PacketSender sender) {
        this.sender = Objects.requireNonNull(sender, "sender");
    }

    @Override
    public Object addOrUpdate(TabEntry entry) {
        Objects.requireNonNull(entry, "entry");
        EnumSet<Action> actions = EnumSet.of(
                Action.ADD_PLAYER, Action.UPDATE_LISTED, Action.UPDATE_DISPLAY_NAME, Action.UPDATE_LIST_ORDER);
        Entry built = new Entry(
                entry.id(),
                profileFor(entry),
                true,
                0,
                GameType.DEFAULT_MODE,
                Components.asVanilla(entry.displayName()),
                true,
                entry.listOrder(),
                null);
        return packet(actions, built);
    }

    @Override
    public Object displayName(UUID id, Component name) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Entry built = entry(id, b -> b.displayName(Components.asVanilla(name)));
        return packet(EnumSet.of(Action.UPDATE_DISPLAY_NAME), built);
    }

    @Override
    public Object listOrder(UUID id, int order) {
        Objects.requireNonNull(id, "id");
        Entry built = entry(id, b -> b.listOrder(order));
        return packet(EnumSet.of(Action.UPDATE_LIST_ORDER), built);
    }

    @Override
    public Object remove(List<UUID> ids) {
        Objects.requireNonNull(ids, "ids");
        return new ClientboundPlayerInfoRemovePacket(List.copyOf(ids));
    }

    @Override
    public void send(Player viewer, Object packet) {
        sender.send(viewer, packet);
    }

    /** The {@code GameProfile} for an add-entry, carrying the skin as a {@code textures} property when present. */
    private static GameProfile profileFor(TabEntry entry) {
        GameProfile profile = new GameProfile(entry.id(), entry.profileName());
        TabSkin skin = entry.skin();
        if (skin != null) {
            profile.properties().put("textures", new Property("textures", skin.textureValue(), skin.signature()));
        }
        return profile;
    }

    /** Build a single-entry update packet through Paper's public {@code (actions, entry)} list constructor. */
    private static ClientboundPlayerInfoUpdatePacket packet(EnumSet<Action> actions, Entry entry) {
        return new ClientboundPlayerInfoUpdatePacket(actions, List.of(entry));
    }

    /**
     * An {@code Entry} for {@code id} that only an update-single action will read: the unread components carry
     * vanilla defaults ({@code listed=true}, latency 0, default game mode, no chat session), and {@code mutator}
     * sets the one field the action does read.
     */
    private static Entry entry(UUID id, java.util.function.UnaryOperator<EntryFields> mutator) {
        return mutator.apply(new EntryFields(id)).build();
    }

    /** A tiny mutable holder so the per-action builders read as one expression without re-listing nine fields. */
    private static final class EntryFields {
        private final UUID id;
        private net.minecraft.network.chat.@Nullable Component displayName;
        private int listOrder;

        private EntryFields(UUID id) {
            this.id = id;
        }

        private EntryFields displayName(net.minecraft.network.chat.Component value) {
            this.displayName = value;
            return this;
        }

        private EntryFields listOrder(int value) {
            this.listOrder = value;
            return this;
        }

        private Entry build() {
            return new Entry(id, null, true, 0, GameType.DEFAULT_MODE, displayName, true, listOrder, null);
        }
    }
}
