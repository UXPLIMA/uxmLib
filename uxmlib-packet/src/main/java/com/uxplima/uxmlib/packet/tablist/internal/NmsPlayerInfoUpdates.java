package com.uxplima.uxmlib.packet.tablist.internal;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

import com.uxplima.uxmlib.packet.tablist.PlayerInfoUpdates;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry;
import org.jspecify.annotations.Nullable;

/**
 * The NMS-bearing reconstruction behind {@link PlayerInfoUpdates#forceUnlisted}: given a real Mojang-mapped
 * {@code ClientboundPlayerInfoUpdatePacket}, it rebuilds the packet with the {@code listed} flag forced to
 * {@code false} on every {@link Entry} whose uuid the predicate accepts, copying every other component of every
 * entry through unchanged. Quarantining {@code net.minecraft} to this class mirrors {@link NmsTabListPackets}.
 *
 * <p>Built against the Mojang-mapped 1.21.11 dev bundle. The {@code Entry} record carries nine components in
 * declared order — {@code profileId, profile, listed, latency, gameMode, displayName, showHat, listOrder,
 * chatSession} — and is reconstructed component-for-component with only {@code listed} changed, so the rewrite
 * is byte-identical to the original apart from the visibility flag. The packet is rebuilt through Paper's public
 * {@code (EnumSet<Action>, List<Entry>)} constructor with the <em>same</em> action set, so the client reads the
 * same fields it would have, now seeing {@code listed=false} for the suppressed entries.
 */
public final class NmsPlayerInfoUpdates {

    private NmsPlayerInfoUpdates() {}

    /**
     * Rewrite {@code packet} to force {@code listed=false} on every entry matching {@code suppress}; see
     * {@link PlayerInfoUpdates#forceUnlisted} for the contract. Returns {@code null} (forward the original) when
     * {@code packet} is not a {@code ClientboundPlayerInfoUpdatePacket}, its actions do not affect listing, or no
     * entry matched.
     */
    public static @Nullable Object forceUnlisted(Object packet, Predicate<UUID> suppress) {
        Objects.requireNonNull(packet, "packet");
        Objects.requireNonNull(suppress, "suppress");
        if (!(packet instanceof ClientboundPlayerInfoUpdatePacket update)) {
            return null;
        }
        EnumSet<Action> actions = update.actions();
        if (!PlayerInfoUpdates.affectsListing(actionNames(actions))) {
            return null;
        }
        List<Entry> rewritten = rewriteEntries(update.entries(), suppress);
        return rewritten == null ? null : new ClientboundPlayerInfoUpdatePacket(actions, rewritten);
    }

    /**
     * Build the rewritten entry list, or {@code null} when no entry matched (so the caller forwards the original
     * untouched rather than rebuilding an identical packet). A matched entry is reconstructed with
     * {@code listed=false}; an unmatched entry (a filler the caller owns, or any player not being suppressed) is
     * carried through by reference.
     */
    private static @Nullable List<Entry> rewriteEntries(List<Entry> entries, Predicate<UUID> suppress) {
        List<Entry> out = new ArrayList<>(entries.size());
        boolean changed = false;
        for (Entry entry : entries) {
            if (entry.listed() && suppress.test(entry.profileId())) {
                out.add(unlisted(entry));
                changed = true;
            } else {
                out.add(entry);
            }
        }
        return changed ? out : null;
    }

    /** A copy of {@code entry} with only the {@code listed} component flipped to {@code false}. */
    private static Entry unlisted(Entry entry) {
        return new Entry(
                entry.profileId(),
                entry.profile(),
                false,
                entry.latency(),
                entry.gameMode(),
                entry.displayName(),
                entry.showHat(),
                entry.listOrder(),
                entry.chatSession());
    }

    private static List<String> actionNames(EnumSet<Action> actions) {
        List<String> names = new ArrayList<>(actions.size());
        for (Action action : actions) {
            names.add(action.name());
        }
        return names;
    }
}
