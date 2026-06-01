/**
 * What goes in a slot and how it renders. A {@link com.uxplima.uxmlib.gui.item.GuiItem} pairs an icon with
 * a {@link com.uxplima.uxmlib.gui.item.GuiAction} click handler; static, animated, and dynamic variants
 * cover a fixed icon, a frame cycle, and a per-render rebuild. The rebuild sees a
 * {@link com.uxplima.uxmlib.gui.item.RenderContext} (the menu, the viewer, the tick) and an
 * {@link com.uxplima.uxmlib.gui.item.ItemPopulator} fills a page from a backing list. Tweak the rendered
 * {@code ItemStack} without rebuilding it through a {@link com.uxplima.uxmlib.gui.item.DisplayModifier}
 * (factories in {@link com.uxplima.uxmlib.gui.item.DisplayModifiers}).
 */
@NullMarked
package com.uxplima.uxmlib.gui.item;

import org.jspecify.annotations.NullMarked;
