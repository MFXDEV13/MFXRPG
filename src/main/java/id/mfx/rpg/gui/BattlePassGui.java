package id.mfx.rpg.gui;

import id.mfx.rpg.model.BattlePassReward;
import id.mfx.rpg.model.BattlePassTier;
import id.mfx.rpg.service.BattlePassService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class BattlePassGui implements InventoryHolder {

    private final BattlePassService service;
    private final Inventory inventory;

    public BattlePassGui(BattlePassService service) {
        this.service = service;
        this.inventory = Bukkit.createInventory(this, 54, "§8" + service.displayName());
        render();
    }

    public void render() {
        inventory.clear();
        for (BattlePassTier tier : service.tiers()) {
            int column = (tier.level() - 1) % 9;
            int row = ((tier.level() - 1) / 9) * 2;
            if (row + 1 >= 6) break;
            inventory.setItem(row * 9 + column, rewardItem(tier.level(), "§aFree", tier.freeReward()));
            inventory.setItem((row + 1) * 9 + column, rewardItem(tier.level(), "§dPremium", tier.premiumReward()));
        }
    }

    private ItemStack rewardItem(int level, String track, BattlePassReward reward) {
        ItemStack item = new ItemStack(reward == null ? Material.BARRIER : reward.material());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(track + " §7Lv. " + level);
        meta.setLore(reward == null ? List.of("§cTidak dikonfigurasi") : List.of(reward.displayName(), "§eKlik untuk claim"));
        item.setItemMeta(meta);
        return item;
    }

    public BattlePassService service() { return service; }
    @Override public Inventory getInventory() { return inventory; }
}