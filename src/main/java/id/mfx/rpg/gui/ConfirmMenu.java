package id.mfx.rpg.gui;

import id.mfx.rpg.MFXRPG;
import id.mfx.rpg.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.function.Consumer;

public final class ConfirmMenu extends BaseMenu {

    private final String description;
    private final Consumer<Player> confirmed;

    public ConfirmMenu(MFXRPG plugin, String title, String description, Consumer<Player> confirmed) {
        super(plugin, 27, plugin.text().parse(title));
        this.description = description;
        this.confirmed = confirmed;
    }

    @Override
    protected void render(Player player, Inventory inventory) {
        fill(inventory, Material.BLACK_STAINED_GLASS_PANE, "<dark_gray>");
        set(inventory, 13, new ItemBuilder(plugin, Material.PAPER)
                .name("<gradient:#8B5CF6:#22D3EE><bold>KONFIRMASI</bold></gradient>")
                .lore(java.util.List.of(description, "", "<gray>Pastikan keputusanmu sebelum melanjutkan."))
                .build());
        set(inventory, 11, new ItemBuilder(plugin, Material.LIME_CONCRETE)
                .name("<green><bold>✔ KONFIRMASI</bold></green>")
                .lore(java.util.List.of("<gray>Lanjutkan tindakan ini."))
                .action("confirm")
                .build());
        set(inventory, 15, new ItemBuilder(plugin, Material.RED_CONCRETE)
                .name("<red><bold>✘ BATAL</bold></red>")
                .lore(java.util.List.of("<gray>Kembali tanpa perubahan."))
                .action("cancel")
                .build());
    }

    @Override
    public void click(Player player, InventoryClickEvent event, String action, String payload) {
        if (action.equals("confirm")) {
            player.closeInventory();
            confirmed.accept(player);
        } else if (action.equals("cancel")) {
            player.closeInventory();
        }
    }
}