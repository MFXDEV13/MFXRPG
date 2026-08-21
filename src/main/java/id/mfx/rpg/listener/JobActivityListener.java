package id.mfx.rpg.listener;

import id.mfx.rpg.MFXRPG;
import id.mfx.rpg.service.JobService;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;

import java.text.DecimalFormat;
import java.util.Map;

public final class JobActivityListener implements Listener {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
    private final MFXRPG plugin;

    public JobActivityListener(MFXRPG plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        handle(event.getPlayer(), "block-break", event.getBlock().getType().name());
    }

    @EventHandler(ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        Player player = event.getEntity().getKiller();
        if (player != null) {
            handle(player, "entity-kill", event.getEntityType().name());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH || event.getCaught() == null) {
            return;
        }
        String type = event.getCaught().getType().name();
        handle(event.getPlayer(), "fish-catch", type);
    }

    private void handle(Player player, String eventType, String target) {
        plugin.jobService().reward(player, eventType, target).thenAccept(result -> {
            if (!result.rewarded()) {
                return;
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                if (!plugin.rewardService().deposit(player, result.money())) {
                    plugin.text().send(player, plugin.message("common.economy-unavailable"));
                    return;
                }
                if (plugin.jobs().getBoolean("settings.payout.actionbar", true)) {
                    player.sendActionBar(plugin.text().parse("<gold>+" + MONEY.format(result.money()) + " Coins</gold> <dark_gray>┃</dark_gray> <aqua>+" + result.xp() + " Job XP</aqua>"));
                }
                if (result.levelUpJob() != null) {
                    player.showTitle(net.kyori.adventure.title.Title.title(
                            plugin.text().parse("<gradient:#FDE047:#F59E0B><bold>JOB LEVEL UP!</bold></gradient>"),
                            plugin.text().parse("<gray>" + result.levelUpJob().toUpperCase() + " mencapai level <gold>" + result.newLevel() + "</gold></gray>")
                    ));
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9F, 1.1F);
                }
            });
        }).exceptionally(throwable -> {
            plugin.getLogger().warning("Gagal memproses job reward: " + throwable.getMessage());
            return null;
        });
    }
}