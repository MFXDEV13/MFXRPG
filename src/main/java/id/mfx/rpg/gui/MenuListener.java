package id.mfx.rpg.gui;

import id.mfx.rpg.MFXRPG;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public final class MenuListener implements Listener {

    private final MFXRPG plugin;
    private final NamespacedKey actionKey;
    private final NamespacedKey payloadKey;

    public MenuListener(MFXRPG plugin) {
        this.plugin = plugin;
        this.actionKey = new NamespacedKey(plugin, "menu_action");
        this.payloadKey = new NamespacedKey(plugin, "menu_payload");
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof MenuHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(topInventory)) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        String action = clicked.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action == null || action.isBlank()) {
            return;
        }

        String payload = clicked.getItemMeta().getPersistentDataContainer().get(payloadKey, PersistentDataType.STRING);
        holder.menu().click(player, event, action, payload == null ? "" : payload);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }
}