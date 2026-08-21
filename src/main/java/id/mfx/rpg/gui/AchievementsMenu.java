package id.mfx.rpg.gui;

import id.mfx.rpg.MFXRPG;
import id.mfx.rpg.model.AchievementDefinition;
import id.mfx.rpg.util.ItemBuilder;
import id.mfx.rpg.util.ProgressBar;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Map;

public final class AchievementsMenu extends BaseMenu {

    public AchievementsMenu(MFXRPG plugin) {
        super(plugin, 54, plugin.text().parse("<gradient:#FDE047:#F59E0B><bold>✦ ACHIEVEMENTS ✦</bold></gradient>"));
    }

    @Override
    protected void render(Player player, Inventory inventory) {
        fill(inventory, Material.BLACK_STAINED_GLASS_PANE, "<dark_gray>");
        plugin.achievementService().state(player.getUniqueId()).thenAccept(state ->
                plugin.getServer().getScheduler().runTask(plugin, () -> renderLoaded(inventory, state)));
    }

    private void renderLoaded(Inventory inventory, Map<String, long[]> state) {
        int slot = 10;
        for (AchievementDefinition achievement : plugin.achievementService().achievements()) {
            long[] progress = state.getOrDefault(achievement.id(), new long[]{0L, 0L});
            boolean unlocked = progress[1] == 1L;

            List<String> lore = List.of(
                    "<gray>Kategori: <white>" + achievement.category() + "</white>",
                    "<gray>Progress: <aqua>" + progress[0] + "</aqua><dark_gray>/</dark_gray><white>" + achievement.amount() + "</white>",
                    ProgressBar.render(progress[0], achievement.amount(), 16),
                    "<gray>Points: <gold>" + achievement.points() + "</gold>",
                    "",
                    unlocked ? "<green><bold>✔ UNLOCKED</bold>" : "<red><bold>✘ BELUM TERBUKA</bold>"
            );

            Material material = Material.matchMaterial(achievement.icon());
            set(inventory, slot++, new ItemBuilder(plugin, material == null ? Material.PAPER : material)
                    .name(achievement.name())
                    .lore(lore)
                    .build());

            if (slot % 9 == 8) slot += 2;
        }

        set(inventory, 49, new ItemBuilder(plugin, Material.ARROW)
                .name("<yellow><bold>← KEMBALI</bold></yellow>")
                .lore(List.of("<gray>Kembali ke dashboard."))
                .action("back")
                .build());
    }

    @Override
    public void click(Player player, InventoryClickEvent event, String action, String payload) {
        if (action.equals("back")) {
            new MainMenu(plugin).open(player);
        }
    }
}