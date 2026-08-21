package id.mfx.rpg.service;

import id.mfx.rpg.MFXRPG;
import id.mfx.rpg.repository.PlayerRepository;
import id.mfx.rpg.repository.PlayerSnapshot;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class DailyRewardService {

    private final MFXRPG plugin;
    private final PlayerRepository repository;

    public DailyRewardService(MFXRPG plugin, PlayerRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public ZoneId zoneId() {
        return ZoneId.of(plugin.dailyRewards().getString("settings.timezone", "Asia/Jakarta"));
    }

    public CompletableFuture<DailyResult> claim(Player player) {
        return repository.loadOrCreate(player.getUniqueId()).thenCompose(snapshot -> {
            LocalDate today = LocalDate.now(zoneId());
            if (today.equals(snapshot.lastDailyClaim())) {
                return CompletableFuture.completedFuture(DailyResult.failure("<red>Kamu sudah mengklaim hadiah hari ini.</red>"));
            }

            int maxDay = plugin.dailyRewards().getInt("settings.max-streak-day", 7);
            boolean resetOnMiss = plugin.dailyRewards().getBoolean("settings.reset-on-missed-day", true);
            int streak = nextStreak(snapshot, today, resetOnMiss, maxDay);
            ConfigurationSection reward = plugin.dailyRewards().getConfigurationSection("rewards." + streak);
            if (reward == null) {
                return CompletableFuture.completedFuture(DailyResult.failure("<red>Reward streak hari ke-" + streak + " belum dikonfigurasi.</red>"));
            }

            PlayerSnapshot updated = new PlayerSnapshot(
                    snapshot.uuid(), snapshot.rankId(), snapshot.rankXp(), snapshot.prestige(),
                    streak, Math.max(snapshot.longestDailyStreak(), streak), today, snapshot.jobs()
            );

            double money = reward.getDouble("money", 0.0D);
            List<String> commands = reward.getStringList("commands");
            return repository.save(updated).thenApply(ignored -> new DailyResult(true, "", streak, money, commands));
        });
    }

    public int displayStreak(PlayerSnapshot snapshot) {
        LocalDate today = LocalDate.now(zoneId());
        if (snapshot.lastDailyClaim() == null) {
            return 0;
        }
        if (today.equals(snapshot.lastDailyClaim()) || today.minusDays(1).equals(snapshot.lastDailyClaim())) {
            return snapshot.dailyStreak();
        }
        return plugin.dailyRewards().getBoolean("settings.reset-on-missed-day", true) ? 0 : snapshot.dailyStreak();
    }

    private int nextStreak(PlayerSnapshot snapshot, LocalDate today, boolean resetOnMiss, int maxDay) {
        if (snapshot.lastDailyClaim() == null) {
            return 1;
        }
        if (today.minusDays(1).equals(snapshot.lastDailyClaim())) {
            return snapshot.dailyStreak() >= maxDay ? 1 : snapshot.dailyStreak() + 1;
        }
        if (!resetOnMiss) {
            return snapshot.dailyStreak() >= maxDay ? 1 : snapshot.dailyStreak() + 1;
        }
        return 1;
    }

    public record DailyResult(boolean success, String message, int streak, double money, List<String> commands) {
        public static DailyResult failure(String message) {
            return new DailyResult(false, message, 0, 0.0D, List.of());
        }
    }
}