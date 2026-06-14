package com.uxplima.uxmlib.packet.tablist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.UUID;

import net.kyori.adventure.text.Component;

import org.junit.jupiter.api.Test;

/** Value-semantics of the pure tab-list types: construction, the documented defaults, and the input guards. */
class TabEntryTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    @Test
    void profileNameDefaultsToTheIdStringWhenNameIsNull() {
        TabEntry entry = new TabEntry(ID, Component.text("hi"), 0, null, null);

        assertThat(entry.name()).isNull();
        assertThat(entry.profileName()).isEqualTo(ID.toString());
    }

    @Test
    void profileNameUsesTheExplicitNameWhenSet() {
        TabEntry entry = new TabEntry(ID, Component.text("hi"), 0, null, "Steve");

        assertThat(entry.profileName()).isEqualTo("Steve");
    }

    @Test
    void ofBuildsAMinimalEntry() {
        Component name = Component.text("row");
        TabEntry entry = TabEntry.of(ID, name);

        assertThat(entry.id()).isEqualTo(ID);
        assertThat(entry.displayName()).isEqualTo(name);
        assertThat(entry.listOrder()).isZero();
        assertThat(entry.skin()).isNull();
        assertThat(entry.name()).isNull();
        assertThat(entry.profileName()).isEqualTo(ID.toString());
    }

    @Test
    void carriesListOrderAndSkin() {
        TabSkin skin = new TabSkin("base64value", "sig");
        TabEntry entry = new TabEntry(ID, Component.text("x"), 42, skin, "Alex");

        assertThat(entry.listOrder()).isEqualTo(42);
        assertThat(entry.skin()).isEqualTo(skin);
    }

    @Test
    void rejectsNullRequiredFields() {
        assertThatNullPointerException().isThrownBy(() -> new TabEntry(nullUuid(), Component.text("x"), 0, null, null));
        assertThatNullPointerException().isThrownBy(() -> new TabEntry(ID, nullComponent(), 0, null, null));
    }

    @SuppressWarnings("NullAway") // intentionally feeds null to assert the constructor guard fires.
    private static UUID nullUuid() {
        return null;
    }

    @SuppressWarnings("NullAway") // intentionally feeds null to assert the constructor guard fires.
    private static Component nullComponent() {
        return null;
    }
}
