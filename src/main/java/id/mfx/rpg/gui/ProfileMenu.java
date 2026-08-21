package id.mfx.rpg.gui;

import id.mfx.rpg.MFXRPG;
import id.mfx.rpg.repository.PlayerSnapshot;
import id.mfx.rpg.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;

public final class ProfileMenu extends BaseMenu {

    private final OfflinePlayer target;

    public ProfileMenu(MFXRPG plugin, OfflinePlayer target) {
        super(plugin, 27, plugin.text().parse("<gradient:#8B5CF6:#22D3EE><bold>✦ PROFIL ✦</bold></gradient>"));
        this.target = target;
    }

    @Override
    protected void render(Player viewer, Inventory inventory) {
        fill(inventory, Material.BLACK_STAINED_GLASS_PANE, "<dark_gray>");

        plugin.rankService().load(target.getUniqueId()).thenAccept(snapshot ->
                plugin.achievementService().state(target.getUniqueId()).thenAccept(achievementState ->
                        plugin.getServer().getScheduler().runTask(plugin, () -> renderLoaded(inventory, snapshot, achievementState))
                )
        );
    }

    private void renderLoaded(Inventory inventory, PlayerSnapshot snapshot, java.util.Map<String, long[]> achievementState) {
        long totalPoints = 0;
        long unlockedCount = 0;
        for (var achievement : plugin.achievementService().achievements()) {
            long[] state = achievementState.getOrDefault(achievement.id(), new long[]{0L, 0L});
            if (state[1] == 1L) {
                totalPoints += achievement.points();
                unlockedCount++;
            }
        }

        String rankName = plugin.rankService().rank(snapshot.rankId()).map(rank -> rank.displayName()).orElse(snapshot.rankId());

        set(inventory, 13, new ItemBuilder(plugin, Material.PLAYER_HEAD)
                .name("<gradient:#8B5CF6:#22D3EE><bold>" + target.getName() + "</bold></gradient>")
                .lore(List.of(
                        "<dark_gray>────────────────────",
                        "<gray>Rank: " + rankName,
                        "<gray>Prestige: <gold>" + snapshot.prestige() + "</gold>",
                        "<gray>Daily Streak: <yellow>" + snapshot.dailyStreak() + " hari</yellow>",
                        "<gray>Achievement: <aqua>" + unlockedCount + "</aqua> unlocked",
                        "<gray>Achievement Points: <gold>" + totalPoints + "</gold>",
                        "<dark_gray>────────────────────"
                ))
                .build());

        set(inventory, 22, new ItemBuilder(plugin, Material.ARROW)
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