/**
 * The packet tab-list layer: a pure port ({@link com.uxplima.uxmlib.packet.tablist.TabListPackets}) plus the
 * value types it speaks in ({@link com.uxplima.uxmlib.packet.tablist.TabEntry},
 * {@link com.uxplima.uxmlib.packet.tablist.TabSkin}). It builds per-viewer tab-list rows — custom display
 * name, client-side sort order, custom skin — the things native Paper cannot do per viewer. Everything here is
 * NMS-free; the single Mojang-mapped implementation sits in {@code tablist.internal}.
 */
@NullMarked
package com.uxplima.uxmlib.packet.tablist;

import org.jspecify.annotations.NullMarked;
