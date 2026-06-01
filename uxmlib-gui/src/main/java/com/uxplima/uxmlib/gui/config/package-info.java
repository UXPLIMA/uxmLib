/**
 * Building a menu from a config file. {@link com.uxplima.uxmlib.gui.config.MenuConfig} reads a HOCON node
 * — title, size, and per-slot icons — into a ready {@link com.uxplima.uxmlib.gui.SimpleGui}, so a server
 * owner re-skins a menu in a file while code only wires the click behaviour;
 * {@link com.uxplima.uxmlib.gui.config.MenuActions} maps the action names that appear in such a file to the
 * {@link com.uxplima.uxmlib.gui.GuiNavigator} verbs they trigger.
 */
@NullMarked
package com.uxplima.uxmlib.gui.config;

import org.jspecify.annotations.NullMarked;
