package id.mfx.rpg.gui;

import id.mfx.rpg.MFXRPG;
import id.mfx.rpg.repository.PlayerSnapshot;
import id.mfx.rpg.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class DailyMenu extends BaseMenu {

    public DailyMenu(MFXRPG plugin) {
        super(plugin, 45, plugin.text().parse("<gradient:#FACC15:#FB923C><bold>✦ DAILY REWARD ✦</bold></gradient>"));
    }

    @Override
    protected void render(Player player, Inventory inventory) {
        fill(inventory, Material.BLACK_STAINED_GLASS_PANE, "<dark_gray>");
        plugin.playerRepository().loadOrCreate(player.getUniqueId()).thenAccept(snapshot -> plugin.getServer().getScheduler().runTask(plugin, () -> renderLoaded(player, inventory, snapshot)));
    }

    private void renderLoaded(Player player, Inventory inventory, PlayerSnapshot snapshot) {
        int displayedStreak = plugin.dailyRewardService().displayStreak(snapshot);
        boolean claimedToday = LocalDate.now(plugin.dailyRewardService().zoneId()).equals(snapshot.lastDailyClaim());

        set(inventory, 4, new ItemBuilder(plugin, Material.CLOCK)
                .name("<gradient:#FACC15:#FB923C><bold>STREAK HARIAN</bold></gradient>")
                .lore(List.of(
                        "<gray>Streak saat ini: <gold><bold>" + displayedStreak + " hari</bold></gold>",
                        "<gray>Streak terpanjang: <yellow>" + snapshot.longestDailyStreak() + " hari</yellow>",
                        "",
                        claimedToday ? "<green><bold>✔ Sudah diklaim hari ini</bold>" : "<gold><bold>▶ Hadiah tersedia untuk diklaim</bold>"
                ))
                .action(claimedToday ? "none" : "claim")
                .build());

        for (int day = 1; day <= 7; day++) {
            ConfigurationSection reward = plugin.dailyRewards().getConfigurationSection("rewards." + day);
            if (reward == null) {
                continue;
            }
            boolean past = day < (claimedToday ? displayedStreak : Math.max(1, displayedStreak + 1));
            boolean today = !claimedToday && day == (displayedStreak >= 7 ? 1 : displayedStreak + 1);
            Material material = Material.matchMaterial(reward.getString("icon", "CHEST"));
            List<String> lore = new ArrayList<>();
            lore.add("<gray>Coins: <gold>" + reward.getDouble("money") + "</gold>");
            lore.add("<dark_gray>────────────────────");
            lore.add(past ? "<green><bold>✔ SUDAH DIKLAIM</bold>" : today ? "<gold><bold>✦ TERSEDIA HARI INI</bold>" : "<red><bold>✘ TERKUNCI</bold>");
            set(inventory, 10 + day * 3, new ItemBuilder(plugin, material == null ? Material.CHEST : material)
                    .name(reward.getString("name", "<gold>Day " + day + "</gold>"))
                    .lore(lore)
                    .action(today ? "claim" : "none")
                    .build());
        }

        set(inventory, 40, new ItemBuilder(plugin, Material.ARROW)
                .name("<yellow><bold>← KEMBALI</bold></yellow>")
                .lore(List.of("<gray>Kembali ke dashboard."))
                .action("back")
                .build());
    }

    @Override
    public void click(Player player, InventoryClickEvent event, String action, String payload) {
        if (action.equals("back")) {
            new MainMenu(plugin).open(player);
            return;
        }
        if (!action.equals("claim")) {
            return;
        }

        plugin.dailyRewardService().claim(player).thenAccept(result -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!result.success()) {
                plugin.text().send(player, result.message());
                new DailyMenu(plugin).open(player);
                return;
            }
            if (!plugin.rewardService().deposit(player, result.money())) {
                plugin.text().send(player, plugin.message("common.economy-unavailable"));
                return;
            }
            plugin.rewardService().executeConsoleCommands(player, result.commands());
            player.showTitle(net.kyori.adventure.title.Title.title(
                    plugin.text().parse("<gradient:#FACC15:#FB923C><bold>HADIAH DIKLAIM!</bold></gradient>"),
                    plugin.text().parse("<gray>Streak hari ke-<gold>" + result.streak() + "</gold></gray>")
            ));
            new DailyMenu(plugin).open(player);
        }));
    }
}