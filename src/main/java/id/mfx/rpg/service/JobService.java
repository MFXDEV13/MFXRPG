package id.mfx.rpg.service;

import id.mfx.rpg.MFXRPG;
import id.mfx.rpg.model.JobDefinition;
import id.mfx.rpg.model.PlayerJobData;
import id.mfx.rpg.repository.PlayerRepository;
import id.mfx.rpg.repository.PlayerSnapshot;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class JobService {

    private final MFXRPG plugin;
    private final PlayerRepository repository;
    private final Map<String, JobDefinition> jobs = new LinkedHashMap<>();

    public JobService(MFXRPG plugin, PlayerRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
        reload();
    }

    public void reload() {
        jobs.clear();
        ConfigurationSection allJobs = plugin.jobs().getConfigurationSection("jobs");
        if (allJobs == null) {
            return;
        }

        for (String id : allJobs.getKeys(false)) {
            ConfigurationSection section = allJobs.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            jobs.put(id, new JobDefinition(
                    id,
                    section.getString("display-name", id),
                    section.getString("icon", "STONE"),
                    section.getStringList("description"),
                    loadRewards(section.getConfigurationSection("actions.block-break")),
                    loadRewards(section.getConfigurationSection("actions.entity-kill")),
                    loadRewards(section.getConfigurationSection("actions.fish-catch"))
            ));
        }
    }

    private Map<String, JobDefinition.Reward> loadRewards(ConfigurationSection section) {
        Map<String, JobDefinition.Reward> rewards = new HashMap<>();
        if (section == null) {
            return rewards;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection reward = section.getConfigurationSection(key);
            if (reward != null) {
                rewards.put(key.toUpperCase(), new JobDefinition.Reward(reward.getDouble("money"), reward.getLong("xp")));
            }
        }
        return rewards;
    }

    public List<JobDefinition> jobs() {
        return List.copyOf(jobs.values());
    }

    public Optional<JobDefinition> job(String id) {
        return Optional.ofNullable(jobs.get(id.toLowerCase()));
    }

    public long requiredXp(int level) {
        double base = plugin.jobs().getDouble("settings.level.base-xp", 100.0D);
        double growth = plugin.jobs().getDouble("settings.level.growth-per-level", 1.35D);
        return Math.max(1L, Math.round(base * Math.pow(growth, Math.max(0, level - 1))));
    }

    public CompletableFuture<ActionResult> join(Player player, String jobId) {
        Optional<JobDefinition> definition = job(jobId);
        if (definition.isEmpty()) {
            return CompletableFuture.completedFuture(ActionResult.failure("<red>Job tersebut tidak ditemukan.</red>"));
        }

        return repository.loadOrCreate(player.getUniqueId()).thenCompose(snapshot -> {
            if (snapshot.jobs().containsKey(jobId)) {
                return CompletableFuture.completedFuture(ActionResult.failure("<yellow>Kamu sudah bergabung dengan job ini.</yellow>"));
            }

            int max = plugin.jobs().getInt("settings.max-active-jobs", 1);
            if (snapshot.jobs().size() >= max) {
                return CompletableFuture.completedFuture(ActionResult.failure("<red>Kamu hanya dapat memiliki " + max + " job aktif.</red>"));
            }

            Map<String, PlayerJobData> jobs = new HashMap<>(snapshot.jobs());
            jobs.put(jobId, new PlayerJobData(jobId, 1, 0L, 0.0D));
            PlayerSnapshot updated = copy(snapshot, jobs);
            return repository.save(updated).thenApply(ignored -> ActionResult.success("<green>Kamu bergabung sebagai " + definition.get().displayName() + "<green>.</green>"));
        });
    }

    public CompletableFuture<ActionResult> leave(Player player, String jobId) {
        return repository.loadOrCreate(player.getUniqueId()).thenCompose(snapshot -> {
            if (!snapshot.jobs().containsKey(jobId)) {
                return CompletableFuture.completedFuture(ActionResult.failure("<red>Kamu tidak sedang bergabung dengan job tersebut.</red>"));
            }
            Map<String, PlayerJobData> jobs = new HashMap<>(snapshot.jobs());
            jobs.remove(jobId);
            return repository.save(copy(snapshot, jobs)).thenApply(ignored -> ActionResult.success("<yellow>Kamu keluar dari job <white>" + jobId + "</white>.</yellow>"));
        });
    }

    public CompletableFuture<RewardResult> reward(Player player, String eventType, String target) {
        return repository.loadOrCreate(player.getUniqueId()).thenCompose(snapshot -> {
            Map<String, PlayerJobData> updatedJobs = new HashMap<>(snapshot.jobs());
            double totalMoney = 0.0D;
            long totalXp = 0L;
            String levelUpJob = null;
            int newLevel = 0;

            for (PlayerJobData jobData : snapshot.jobs().values()) {
                JobDefinition definition = jobs.get(jobData.jobId());
                if (definition == null) {
                    continue;
                }
                JobDefinition.Reward reward = switch (eventType) {
                    case "block-break" -> definition.blockBreakRewards().get(target.toUpperCase());
                    case "entity-kill" -> definition.entityKillRewards().get(target.toUpperCase());
                    case "fish-catch" -> definition.fishCatchRewards().get(target.toUpperCase());
                    default -> null;
                };
                if (reward == null) {
                    continue;
                }

                int prestige = snapshot.prestige();
                double prestigeBonus = plugin.ranks().getDouble("settings.prestige.permanent-job-xp-bonus-per-level", 0.05D) * prestige;
                long gainedXp = Math.max(1L, Math.round(reward.xp() * (1.0D + prestigeBonus)));
                int level = jobData.level();
                long xp = jobData.xp() + gainedXp;
                boolean leveled = false;
                while (xp >= requiredXp(level)) {
                    xp -= requiredXp(level);
                    level++;
                    leveled = true;
                }

                updatedJobs.put(jobData.jobId(), new PlayerJobData(jobData.jobId(), level, xp, jobData.totalEarnings() + reward.money()));
                totalMoney += reward.money();
                totalXp += gainedXp;
                if (leveled) {
                    levelUpJob = jobData.jobId();
                    newLevel = level;
                }
            }

            if (totalXp == 0L) {
                return CompletableFuture.completedFuture(RewardResult.none());
            }

            PlayerSnapshot updated = copy(snapshot, updatedJobs);
            String finalLevelUpJob = levelUpJob;
            int finalNewLevel = newLevel;
            double finalMoney = totalMoney;
            long finalXp = totalXp;
            return repository.save(updated).thenApply(ignored -> new RewardResult(true, finalMoney, finalXp, finalLevelUpJob, finalNewLevel));
        });
    }

    private PlayerSnapshot copy(PlayerSnapshot source, Map<String, PlayerJobData> jobs) {
        return new PlayerSnapshot(source.uuid(), source.rankId(), source.rankXp(), source.prestige(),
                source.dailyStreak(), source.longestDailyStreak(), source.lastDailyClaim(), Map.copyOf(jobs));
    }

    public record ActionResult(boolean success, String message) {
        public static ActionResult success(String message) { return new ActionResult(true, message); }
        public static ActionResult failure(String message) { return new ActionResult(false, message); }
    }

    public record RewardResult(boolean rewarded, double money, long xp, String levelUpJob, int newLevel) {
        public static RewardResult none() { return new RewardResult(false, 0.0D, 0L, null, 0); }
    }
}