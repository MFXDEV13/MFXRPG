package id.mfx.rpg.listener;

import id.mfx.rpg.MFXRPG;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;

public final class ProgressListener implements Listener {

    private final MFXRPG plugin;

    public ProgressListener(MFXRPG plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        process(event.getPlayer(), "BLOCK_BREAK", event.getBlock().getType().name());
    }

    @EventHandler(ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        Player player = event.getEntity().getKiller();
        if (player != null) {
            process(player, "ENTITY_KILL", event.getEntityType().name());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH || event.getCaught() == null) return;
        process(event.getPlayer(), "FISH_CATCH", event.getCaught().getType().name());
    }

    private void process(Player player, String eventType, String target) {
        plugin.questService().addProgress(player, eventType, target).thenAccept(result ->
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (player.isOnline() && !result.completedQuestIds().isEmpty()) {
                        player.sendActionBar(plugin.text().parse("<gradient:#34D399:#22D3EE><bold>QUEST SELESAI!</bold></gradient> <gray>Klaim di /quests</gray>"));
                        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8F, 1.3F);
                    }
                })
        ).exceptionally(throwable -> {
            plugin.getLogger().warning("Gagal memproses quest progress: " + throwable.getMessage());
            return null;
        });

        plugin.achievementService().addProgress(player, eventType, target).thenAccept(unlocked ->
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;
                    for (String achievementId : unlocked) {
                        plugin.achievementService().byId(achievementId).ifPresent(definition -> {
                            player.showTitle(net.kyori.adventure.title.Title.title(
                                    plugin.text().parse("<gradient:#FACC15:#FB923C><bold>ACHIEVEMENT UNLOCKED!</bold></gradient>"),
                                    plugin.text().parse(definition.name())
                            ));
                            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
                            if (definition.rewardMoney() > 0) {
                                plugin.rewardService().deposit(player, definition.rewardMoney());
                            }
                        });
                    }
                })
        ).exceptionally(throwable -> {
            plugin.getLogger().warning("Gagal memproses achievement progress: " + throwable.getMessage());
            return null;
        });
    }
}