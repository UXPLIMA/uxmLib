package com.uxplima.uxmlib.gui.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;

import com.uxplima.uxmlib.gui.Guis;
import com.uxplima.uxmlib.gui.SimpleGui;
import com.uxplima.uxmlib.gui.item.GuiItem;
import com.uxplima.uxmlib.item.ItemBuilder;
import com.uxplima.uxmlib.text.Text;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

/**
 * Builds a {@link SimpleGui} from a HOCON/Configurate node, so a server owner can lay out and re-skin a
 * menu in a config file while code keeps the behaviour. The node carries a {@code title}, a {@code rows}
 * count, a {@code mask} (a list of nine-character rows), and an {@code items} table whose keys are the
 * mask characters; each item gives a {@code material}, optional MiniMessage {@code name} and {@code lore},
 * and an optional {@code action} resolved from a {@link MenuActions} registry.
 *
 * <pre>{@code
 * title = "<gold>Shop"
 * rows = 3
 * mask = [ "XXXXXXXXX", "X   C   X", "XXXXXXXXX" ]
 * items {
 *   X { material = "GRAY_STAINED_GLASS_PANE", name = " " }
 *   C { material = "BARRIER", name = "<red>Close", action = "close" }
 * }
 * }</pre>
 */
public final class MenuConfig {

    private MenuConfig() {}

    /** Build a menu from {@code node}, wiring item actions through {@code actions}. */
    public static SimpleGui load(ConfigurationNode node, MenuActions actions) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(actions, "actions");
        Component title = Text.mini(node.node("title").getString(""));
        int rows = node.node("rows").getInt(determineRows(node));
        SimpleGui gui = Guis.gui().title(title).rows(rows).build();
        Map<Character, GuiItem> legend = readLegend(node.node("items"), actions);
        gui.filler().pattern(readMask(node.node("mask")), legend);
        return gui;
    }

    private static int determineRows(ConfigurationNode node) {
        int maskRows = node.node("mask").childrenList().size();
        return Math.min(6, Math.max(1, maskRows));
    }

    private static List<String> readMask(ConfigurationNode maskNode) {
        List<String> mask = new ArrayList<>();
        for (ConfigurationNode row : maskNode.childrenList()) {
            mask.add(row.getString(""));
        }
        return mask;
    }

    private static Map<Character, GuiItem> readLegend(ConfigurationNode itemsNode, MenuActions actions) {
        Map<Character, GuiItem> legend = new HashMap<>();
        for (Map.Entry<Object, ? extends ConfigurationNode> entry :
                itemsNode.childrenMap().entrySet()) {
            String key = String.valueOf(entry.getKey());
            if (key.isEmpty()) {
                continue;
            }
            legend.put(key.charAt(0), readItem(entry.getValue(), actions));
        }
        return legend;
    }

    private static GuiItem readItem(ConfigurationNode itemNode, MenuActions actions) {
        ItemStack icon = buildIcon(itemNode);
        String actionName = itemNode.node("action").getString();
        if (actionName != null) {
            java.util.function.Consumer<org.bukkit.event.inventory.InventoryClickEvent> action =
                    actions.get(actionName);
            if (action == null) {
                throw new IllegalArgumentException("unknown menu action: " + actionName);
            }
            return GuiItem.button(icon, action);
        }
        return GuiItem.display(icon);
    }

    private static ItemStack buildIcon(ConfigurationNode itemNode) {
        Material material = parseMaterial(itemNode.node("material").getString("STONE"));
        ItemBuilder builder = ItemBuilder.of(material);
        String name = itemNode.node("name").getString();
        if (name != null) {
            builder.name(Text.mini(name));
        }
        List<Component> lore = readLore(itemNode.node("lore"));
        if (!lore.isEmpty()) {
            builder.lore(lore);
        }
        int amount = itemNode.node("amount").getInt(1);
        if (amount > 1) {
            builder.amount(amount);
        }
        return builder.build();
    }

    private static List<Component> readLore(ConfigurationNode loreNode) {
        List<Component> lore = new ArrayList<>();
        try {
            List<String> lines = loreNode.getList(String.class);
            if (lines != null) {
                for (String line : lines) {
                    lore.add(Text.mini(line));
                }
            }
        } catch (SerializationException ignored) {
            // A non-list lore node is simply treated as absent.
        }
        return lore;
    }

    private static Material parseMaterial(String raw) {
        Material material = Material.matchMaterial(raw);
        if (material == null) {
            throw new IllegalArgumentException("unknown material in menu config: " + raw);
        }
        return material;
    }
}
