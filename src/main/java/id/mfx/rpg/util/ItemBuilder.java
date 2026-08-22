package id.mfx.rpg.util;

import id.mfx.rpg.MFXRPG;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ItemBuilder {

    private final MFXRPG plugin;
    private final TextService text;
    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(MFXRPG plugin, Material material) {
        this.plugin = plugin;
        this.text = plugin.text();
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder itemModel(String namespace, String key) {
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(new NamespacedKey(namespace, key));
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder name(String miniMessage) {
        meta.displayName(text.parse(miniMessage));
        return this;
    }

    public ItemBuilder name(String miniMessage, Map<String, Component> placeholders) {
        meta.displayName(text.parse(miniMessage, placeholders));
        return this;
    }

    public ItemBuilder lore(List<String> miniMessageLines) {
        List<Component> lines = new ArrayList<>();
        for (String line : miniMessageLines) {
            lines.add(text.parse(line));
        }
        meta.lore(lines);
        return this;
    }

    public ItemBuilder lore(List<String> miniMessageLines, Map<String, Component> placeholders) {
        List<Component> lines = new ArrayList<>();
        for (String line : miniMessageLines) {
            lines.add(text.parse(line, placeholders));
        }
        meta.lore(lines);
        return this;
    }

    public ItemBuilder action(String action) {
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "menu_action"),
                PersistentDataType.STRING,
                action
        );
        return this;
    }

    public ItemBuilder payload(String payload) {
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "menu_payload"),
                PersistentDataType.STRING,
                payload
        );
        return this;
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}