package id.mfx.rpg.gui;

import id.mfx.rpg.MFXRPG;
import id.mfx.rpg.model.QuestDefinition;
import id.mfx.rpg.util.ItemBuilder;
import id.mfx.rpg.util.ProgressBar;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Map;

public final class QuestsMenu extends BaseMenu {

    public QuestsMenu(MFXRPG plugin) {
        super(plugin, 45, plugin.text().parse("<gradient:#34D399:#22D3EE><bold>✦ QUESTS ✦</bold></gradient>"));
    }

    @Override
    protected void render(Player player, Inventory inventory) {
        fill(inventory, Material.BLACK_STAINED_GLASS_PANE, "<dark_gray>");

        var dailyFuture = plugin.questService().activeQuests(player.getUniqueId(), "DAILY")
                .thenCompose(quests -> plugin.questService().state(player.getUniqueId(), "DAILY")
                        .thenApply(state -> Map.entry(quests, state)));

        var weeklyFuture = plugin.questService().activeQuests(player.getUniqueId(), "WEEKLY")
                .thenCompose(quests -> plugin.questService().state(player.getUniqueId(), "WEEKLY")
                        .thenApply(state -> Map.entry(quests, state)));

        dailyFuture.thenCombine(weeklyFuture, (daily, weekly) -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                renderQuests(inventory, daily.getKey(), daily.getValue(), "DAILY", 10);
                renderQuests(inventory, weekly.getKey(), weekly.getValue(), "WEEKLY", 28);
            });
            return null;
        });

        set(inventory, 40, new ItemBuilder(plugin, Material.ARROW)
                .name("<yellow><bold>← KEMBALI</bold></yellow>")
                .lore(List.of("<gray>Kembali ke dashboard."))
                .action("back")
                .build());
    }

    private void renderQuests(Inventory inventory, List<QuestDefinition> quests, Map<String, long[]> state, String period, int startSlot) {
        int slot = startSlot;
        for (QuestDefinition quest : quests) {
            long[] progress = state.getOrDefault(quest.id(), new long[]{0L, 0L});
            boolean claimed = progress[1] == 1L;
            boolean completed = progress[0] >= quest.amount();

            List<String> lore = List.of(
                    "<gray>Progress: <aqua>" + progress[0] + "</aqua><dark_gray>/</dark_gray><white>" + quest.amount() + "</white>",
                    ProgressBar.render(progress[0], quest.amount(), 16),
                    "<gray>Reward: <gold>" + quest.rewardMoney() + " Coins</gold> <aqua>+" + quest.rewardJobXp() + " Job XP</aqua>",
                    "",
                    claimed ? "<green><bold>✔ SUDAH DIKLAIM</bold>" : completed ? "<gold><bold>▶ Klik untuk klaim</bold>" : "<red><bold>✘ BELUM SELESAI</bold>"
            );

            Material material = Material.matchMaterial(quest.icon());
            set(inventory, slot++, new ItemBuilder(plugin, material == null ? Material.PAPER : material)
                    .name(quest.name())
                    .lore(lore)
                    .action(claimed || !completed ? "none" : "claim")
                    .payload(period + ":" + quest.id())
                    .build());
        }
    }

    @Override
    public void click(Player player, InventoryClickEvent event, String action, String payload) {
        if (action.equals("back")) {
            new MainMenu(plugin).open(player);
            return;
        }
        if (!action.equals("claim")) return;

        String[] parts = payload.split(":", 2);
        if (parts.length != 2) return;

        plugin.questService().claim(player, parts[0], parts[1]).thenAccept(result -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!result.success()) {
                plugin.text().send(player, result.message());
                return;
            }
            plugin.rewardService().deposit(player, result.money());
            plugin.text().send(player, "<green>Quest selesai! <gold>+" + result.money() + " Coins</gold></green>");
            new QuestsMenu(plugin).open(player);
        }));
    }
}