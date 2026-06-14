package com.uxplima.uxmlib.nametag.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.entity.Player;

import io.papermc.paper.adventure.PaperAdventure;

import net.kyori.adventure.text.Component;

import com.uxplima.uxmlib.nametag.Alignment;
import com.uxplima.uxmlib.nametag.Appearance;
import com.uxplima.uxmlib.nametag.Billboard;
import com.uxplima.uxmlib.nametag.NametagPackets;
import com.uxplima.uxmlib.npc.PacketSender;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The sole NMS-bearing class of the nametag renderer: it builds the real Mojang-mapped packets that paint a
 * text {@code Display} per viewer and writes them through the connection. Quarantining {@code net.minecraft}
 * to one class follows the same precedent as {@code uxmlib-npc}'s {@code ChannelResolver}, which isolates the
 * unavoidable server-internal reach so the rest of the module stays pure and unit-testable against a fake.
 *
 * <p>Built against the Mojang-mapped 1.21.11 dev bundle; Paper's runtime remapper maps these back to the
 * server's own mappings at load. A handful of the {@code Display}/{@code TextDisplay} data-watcher accessors
 * the metadata packet needs are package-private static fields, so they are read once at construction through a
 * single guarded reflection helper (the accessor object carries its own network id, which keeps us from
 * hard-coding the volatile integer indices). Everything else uses public constructors and the public
 * stream codecs the bundle exposes.
 */
public final class NmsNametagPackets implements NametagPackets {

    private final PacketSender sender;

    private final EntityDataAccessor<net.minecraft.network.chat.Component> textAccessor;
    private final EntityDataAccessor<Byte> billboardAccessor;
    private final EntityDataAccessor<Integer> backgroundAccessor;
    private final EntityDataAccessor<Byte> textOpacityAccessor;
    private final EntityDataAccessor<Byte> styleFlagsAccessor;
    private final EntityDataAccessor<Integer> lineWidthAccessor;
    private final EntityDataAccessor<Float> viewRangeAccessor;
    private final EntityDataAccessor<org.joml.Vector3fc> translationAccessor;
    private final EntityDataAccessor<org.joml.Vector3fc> scaleAccessor;
    private final EntityDataAccessor<Integer> interpolationDurationAccessor;
    private final EntityDataAccessor<Integer> interpolationStartDeltaAccessor;

    public NmsNametagPackets(PacketSender sender) {
        this.sender = Objects.requireNonNull(sender, "sender");
        this.textAccessor = accessor(Display.TextDisplay.class, "DATA_TEXT_ID");
        this.billboardAccessor = accessor(Display.class, "DATA_BILLBOARD_RENDER_CONSTRAINTS_ID");
        this.backgroundAccessor = accessor(Display.TextDisplay.class, "DATA_BACKGROUND_COLOR_ID");
        this.textOpacityAccessor = accessor(Display.TextDisplay.class, "DATA_TEXT_OPACITY_ID");
        this.styleFlagsAccessor = accessor(Display.TextDisplay.class, "DATA_STYLE_FLAGS_ID");
        this.lineWidthAccessor = accessor(Display.TextDisplay.class, "DATA_LINE_WIDTH_ID");
        this.viewRangeAccessor = accessor(Display.class, "DATA_VIEW_RANGE_ID");
        this.translationAccessor = accessor(Display.class, "DATA_TRANSLATION_ID");
        this.scaleAccessor = accessor(Display.class, "DATA_SCALE_ID");
        this.interpolationDurationAccessor = accessor(Display.class, "DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID");
        this.interpolationStartDeltaAccessor =
                accessor(Display.class, "DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID");
    }

    @Override
    public int allocateEntityId() {
        // The shared server counter; ids it hands out never collide with a real entity.
        return Entity.nextEntityId();
    }

    @Override
    public Object spawnPacket(int entityId, double x, double y, double z) {
        return new ClientboundAddEntityPacket(
                entityId, new UUID(0L, entityId), x, y, z, 0.0f, 0.0f, EntityType.TEXT_DISPLAY, 0, Vec3.ZERO, 0.0);
    }

    @Override
    public Object metadataPacket(
            int entityId, Component text, Appearance appearance, int opacity, Vector3f translation) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(appearance, "appearance");
        Objects.requireNonNull(translation, "translation");
        if (opacity < 0 || opacity > 255) {
            throw new IllegalArgumentException("opacity must be 0-255, was " + opacity);
        }
        return new ClientboundSetEntityDataPacket(entityId, dataValues(text, appearance, opacity, translation));
    }

    private List<SynchedEntityData.DataValue<?>> dataValues(
            Component text, Appearance appearance, int opacity, Vector3f translation) {
        List<SynchedEntityData.DataValue<?>> values = new ArrayList<>(11);
        values.add(SynchedEntityData.DataValue.create(textAccessor, PaperAdventure.asVanilla(text)));
        values.add(SynchedEntityData.DataValue.create(billboardAccessor, billboardId(appearance.billboard())));
        values.add(SynchedEntityData.DataValue.create(backgroundAccessor, appearance.backgroundArgb()));
        values.add(SynchedEntityData.DataValue.create(textOpacityAccessor, (byte) opacity));
        values.add(SynchedEntityData.DataValue.create(styleFlagsAccessor, styleFlags(appearance)));
        values.add(SynchedEntityData.DataValue.create(lineWidthAccessor, appearance.lineWidth()));
        values.add(SynchedEntityData.DataValue.create(viewRangeAccessor, appearance.viewRange()));
        values.add(SynchedEntityData.DataValue.create(translationAccessor, new Vector3f(translation)));
        values.add(SynchedEntityData.DataValue.create(scaleAccessor, appearance.scale()));
        values.add(SynchedEntityData.DataValue.create(
                interpolationDurationAccessor, appearance.interpolationDurationTicks()));
        // Start the lerp from this very tick so a transform change is applied immediately, not after a delay.
        values.add(SynchedEntityData.DataValue.create(interpolationStartDeltaAccessor, 0));
        return values;
    }

    /** The data-watcher byte for the billboard mode, matching vanilla {@code Display.BillboardConstraints}. */
    private static byte billboardId(Billboard billboard) {
        return switch (billboard) {
            case FIXED -> (byte) 0;
            case VERTICAL -> (byte) 1;
            case HORIZONTAL -> (byte) 2;
            case CENTER -> (byte) 3;
        };
    }

    /** OR together the {@code TextDisplay} style bits the appearance selects. */
    private static byte styleFlags(Appearance appearance) {
        byte flags = 0;
        if (appearance.textShadow()) {
            flags |= Display.TextDisplay.FLAG_SHADOW;
        }
        if (appearance.seeThrough()) {
            flags |= Display.TextDisplay.FLAG_SEE_THROUGH;
        }
        if (appearance.alignment() == Alignment.LEFT) {
            flags |= Display.TextDisplay.FLAG_ALIGN_LEFT;
        } else if (appearance.alignment() == Alignment.RIGHT) {
            flags |= Display.TextDisplay.FLAG_ALIGN_RIGHT;
        }
        return flags;
    }

    @Override
    public Object mountPacket(int vehicleEntityId, int[] passengerIds) {
        Objects.requireNonNull(passengerIds, "passengerIds");
        // The packet exposes only an Entity constructor and a private buffer one, so seat the riders through
        // the public stream codec: write the wire form (vehicle id, then the passenger id array) and decode.
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeVarInt(vehicleEntityId);
            buffer.writeVarIntArray(passengerIds.clone());
            return ClientboundSetPassengersPacket.STREAM_CODEC.decode(buffer);
        } finally {
            buffer.release();
        }
    }

    @Override
    public Object removePacket(int[] entityIds) {
        Objects.requireNonNull(entityIds, "entityIds");
        return new ClientboundRemoveEntitiesPacket(entityIds.clone());
    }

    @Override
    public Object bundle(List<Object> packets) {
        Objects.requireNonNull(packets, "packets");
        List<Packet<? super ClientGamePacketListener>> game = new ArrayList<>(packets.size());
        for (Object packet : packets) {
            game.add(asGamePacket(packet));
        }
        return new ClientboundBundlePacket(game);
    }

    @SuppressWarnings("unchecked") // The renderer only ever feeds this clientbound game packets built above.
    private static Packet<? super ClientGamePacketListener> asGamePacket(Object packet) {
        return (Packet<? super ClientGamePacketListener>) Objects.requireNonNull(packet, "packet");
    }

    @Override
    public void send(Player viewer, Object packet) {
        sender.send(viewer, packet);
    }

    /**
     * Read a {@code Display}/{@code TextDisplay} data-watcher accessor static field by its Mojang-mapped name.
     * Several of these are package-private, so a tiny guarded reflective read is the only way to reach them;
     * failing fast here surfaces a mapping mismatch loudly rather than producing silently broken packets.
     */
    @SuppressWarnings("unchecked") // The field's declared type matches the requested accessor element type.
    private static <T> EntityDataAccessor<T> accessor(Class<?> owner, String field) {
        try {
            var declared = owner.getDeclaredField(field);
            declared.setAccessible(true);
            return (EntityDataAccessor<T>) Objects.requireNonNull(declared.get(null), field);
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new IllegalStateException(
                    "Display accessor " + owner.getName() + "#" + field + " is unavailable on this server", e);
        }
    }
}
