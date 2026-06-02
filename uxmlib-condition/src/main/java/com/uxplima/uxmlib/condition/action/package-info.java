/**
 * A config-driven action engine, the natural pair of the condition engine. A named action string such as
 * {@code [message] <hello>} or {@code [console] give %player% diamond} is parsed <em>once</em> (at load) by
 * {@link com.uxplima.uxmlib.condition.action.ActionParser} into an {@link
 * com.uxplima.uxmlib.condition.action.Action} closure; an {@link
 * com.uxplima.uxmlib.condition.action.ActionList} runs a sequence of them in order against an {@link
 * com.uxplima.uxmlib.condition.action.ActionContext} bundle. The context carries the target audience/player,
 * a console command sink, a command dispatcher seam, and an {@link
 * com.uxplima.uxmlib.condition.OperandResolver} reused from the condition module for placeholder substitution.
 *
 * <p>Delivery is native: message/broadcast/title/action-bar go through the Adventure {@code Audience}
 * ({@code sendMessage}/{@code showTitle}/{@code sendActionBar}), sound through {@code playSound}, console and
 * player commands through {@code Bukkit#dispatchCommand}, and {@code [close]} through the player's inventory.
 * Nothing here depends on the HUD module. Each action declares a sync/async flag (default sync) so a driver
 * can route it through the {@code Scheduler}; this module supplies the flag and the closures, not the
 * scheduling.
 */
@NullMarked
package com.uxplima.uxmlib.condition.action;

import org.jspecify.annotations.NullMarked;
