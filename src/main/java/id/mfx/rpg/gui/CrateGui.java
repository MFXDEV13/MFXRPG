package id.mfx.rpg.gui;

import id.mfx.rpg.model.CrateDefinition;
import id.mfx.rpg.service.CrateService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class CrateGui implements InventoryHolder {

    private final CrateService service;
    private final Inventory inventory;

    public CrateGui(CrateService service) {
        this.service = service;
        this.inventory = Bukkit.createInventory(this, 27, "§8Crates");
        render();
    }

    public void render() {
        inventory.clear();
        int slot = 10;
        for (CrateDefinition crate : service.crates()) {
            ItemStack item = new ItemStack(crate.icon());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(crate.displayName());
            List<String> lore = new ArrayList<>();
            lore.add("§7Key: §f" + crate.keyName());
            lore.add("§7Klik untuk membuka");
            if (crate.pityEnabled()) lore.add("§dPity: " + crate.pityGuaranteeAfterMisses() + " miss");
            meta.setLore(lore);
            item.setItemMeta(meta);
            inventory.setItem(slot++, item);
        }
    }

    public CrateService service() { return service; }
    @Override public Inventory getInventory() { return inventory; }
}