package id.mfx.rpg.listener;

import id.mfx.rpg.gui.BattlePassGui;
import id.mfx.rpg.gui.CrateGui;
import id.mfx.rpg.service.BattlePassService;
import id.mfx.rpg.service.CrateService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class PaketFourGuiListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof CrateGui)
                && !(event.getView().getTopInventory().getHolder() instanceof BattlePassGui)) return;
        if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
            event.setCancelled(true);
        }
        if (!(event.getWhoClicked() instanceof Player player) || event.getRawSlot() < 0
                || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;

        if (event.getView().getTopInventory().getHolder() instanceof CrateGui gui) {
            int index = event.getRawSlot() - 10;
            var crates = gui.service().crates().stream().toList();
            if (index < 0 || index >= crates.size()) return;
            CrateService service = gui.service();
            service.open(player, crates.get(index).id()).thenAccept(result -> {
                if (result.success()) player.sendMessage("§aKamu mendapat " + result.reward().displayName());
                else player.sendMessage("§c" + result.messageKey());
            });
            return;
        }

        if (event.getView().getTopInventory().getHolder() instanceof BattlePassGui gui) {
            int slot = event.getRawSlot();
            int row = slot / 9;
            int column = slot % 9;
            if (row % 2 != 0 && row % 2 != 1) return;
            int level = (row / 2) * 9 + column + 1;
            String track = row % 2 == 0 ? "free" : "premium";
            BattlePassService service = gui.service();
            service.claim(player, level, track).thenAccept(result -> {
                if (result.success()) player.sendMessage("§aReward berhasil diklaim.");
                else player.sendMessage("§c" + result.messageKey());
            });
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof CrateGui)
                && !(event.getView().getTopInventory().getHolder() instanceof BattlePassGui)) return;
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize)) event.setCancelled(true);
    }
}