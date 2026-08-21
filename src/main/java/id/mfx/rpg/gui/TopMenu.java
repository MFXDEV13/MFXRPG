package id.mfx.rpg.gui;

import id.mfx.rpg.MFXRPG;
import id.mfx.rpg.service.LeaderboardService;
import id.mfx.rpg.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;

public final class TopMenu extends BaseMenu {

    public TopMenu(MFXRPG plugin) {
        super(plugin, 45, plugin.text().parse("<gradient:#FACC15:#FB923C><bold>♛ HALL OF FAME ♛</bold></gradient>"));
    }

    @Override
    protected void render(Player player, Inventory inventory) {
        fill(inventory, Material.BLACK_STAINED_GLASS_PANE, "<dark_gray>");

        plugin.leaderboardService().topRank(10).thenAccept(entries ->
                plugin.getServer().getScheduler().runTask(plugin, () -> renderColumn(inventory, entries, 10, "<gradient:#A855F7:#06B6D4><bold>RANK</bold></gradient>", Material.NETHER_STAR)));

        plugin.leaderboardService().topJobEarnings(10).thenAccept(entries ->
                plugin.getServer().getScheduler().runTask(plugin, () -> renderColumn(inventory, entries, 19, "<gradient:#38BDF8:#6366F1><bold>JOB EARNINGS</bold></gradient>", Material.GOLD_INGOT)));

        plugin.leaderboardService().topAchievementPoints(10).thenAccept(entries ->
                plugin.getServer().getScheduler().runTask(plugin, () -> renderColumn(inventory, entries, 28, "<gradient:#FDE047:#F59E0B><bold>ACHIEVEMENT</bold></gradient>", Material.TOTEM_OF_UNDYING)));

        set(inventory, 40, new ItemBuilder(plugin, Material.ARROW)
                .name("<yellow><bold>← KEMBALI</bold></yellow>")
                .lore(List.of("<gray>Kembali ke dashboard."))
                .action("back")
                .build());
    }

    private void renderColumn(Inventory inventory, List<LeaderboardService.Entry> entries, int headerSlot, String title, Material icon) {
        set(inventory, headerSlot, new ItemBuilder(plugin, icon).name(title).build());
        int slot = headerSlot + 1;
        int rank = 1;
        for (LeaderboardService.Entry entry : entries) {
            if (slot % 9 == 8) break;
            set(inventory, slot++, new ItemBuilder(plugin, Material.PAPER)
                    .name("<gray>#" + rank + " <white>" + entry.name() + "</white>")
                    .lore(List.of("<gray>Score: <gold>" + String.format("%,.2f", entry.value()) + "</gold>"))
                    .build());
            rank++;
        }
    }

    @Override
    public void click(Player player, InventoryClickEvent event, String action, String payload) {
        if (action.equals("back")) {
            new MainMenu(plugin).open(player);
        }
    }
}