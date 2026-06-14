package com.uxplima.uxmlib.packet.tablist;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * A player-skin texture, expressed as the two pieces Mojang's profile property carries: the base64-encoded
 * texture value and its optional Yggdrasil signature. This is a pure value — no NMS, no profile type — so the
 * port and its tests stay server-free; the NMS builder turns it into a {@code GameProfile} {@code "textures"}
 * property when it assembles an add-entry packet.
 *
 * @param textureValue the base64-encoded texture payload (the {@code value} of the {@code textures} property)
 * @param signature the property's signature, or {@code null} for an unsigned texture
 */
public record TabSkin(String textureValue, @Nullable String signature) {

    public TabSkin {
        Objects.requireNonNull(textureValue, "textureValue");
    }

    /** A skin with no signature; the client accepts the texture unsigned for synthetic tab entries. */
    public static TabSkin unsigned(String textureValue) {
        return new TabSkin(textureValue, null);
    }
}
