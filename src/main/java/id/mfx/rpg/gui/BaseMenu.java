package id.mfx.rpg.gui;

import id.mfx.rpg.MFXRPG;
import id.mfx.rpg.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public abstract class BaseMenu {

    protected final MFXRPG plugin;
    private final int size;
    private final Component title;

    protected BaseMenu(MFXRPG plugin, int size, Component title) {
        this.plugin = plugin;
        this.size = size;
        this.title = title;
    }

    public final void open(Player player) {
        Inventory inventory = plugin.getServer().createInventory(new MenuHolder(this), size, title);
        render(player, inventory);
        player.openInventory(inventory);
    }

    protected abstract void render(Player player, Inventory inventory);

    public abstract void click(Player player, InventoryClickEvent event, String action, String payload);

    protected void fill(Inventory inventory, Material material, String name) {
        ItemStack filler = new ItemBuilder(plugin, material).name(name).build();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    protected void set(Inventory inventory, int slot, ItemStack item) {
        if (slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, item);
        }
    }
}