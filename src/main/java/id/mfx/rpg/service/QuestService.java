package id.mfx.rpg.service;

import id.mfx.rpg.MFXRPG;
import id.mfx.rpg.model.QuestDefinition;
import id.mfx.rpg.repository.ProgressRepository;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class QuestService {

    private final MFXRPG plugin;
    private final ProgressRepository repository;
    private final Map<String, QuestDefinition> dailyPool = new LinkedHashMap<>();
    private final Map<String, QuestDefinition> weeklyPool = new LinkedHashMap<>();

    public QuestService(MFXRPG plugin, ProgressRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
        reload();
    }

    public void reload() {
        dailyPool.clear();
        weeklyPool.clear();
        loadPool("daily-pool", dailyPool);
        loadPool("weekly-pool", weeklyPool);
    }

    private void loadPool(String key, Map<String, QuestDefinition> target) {
        ConfigurationSection section = plugin.quests().getConfigurationSection(key);
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            ConfigurationSection quest = section.getConfigurationSection(id);
            if (quest == null) continue;
            target.put(id, new QuestDefinition(
                    id,
                    key.equals("daily-pool") ? "DAILY" : "WEEKLY",
                    quest.getString("type", "BLOCK_BREAK"),
                    quest.getString("target", "ANY"),
                    quest.getLong("amount", 1),
                    quest.getString("name", id),
                    quest.getString("icon", "PAPER"),
                    quest.getDouble("reward-money", 0.0D),
                    quest.getLong("reward-job-xp", 0L)
            ));
        }
    }

    public ZoneId zoneId() {
        return ZoneId.of(plugin.quests().getString("settings.timezone", "Asia/Jakarta"));
    }

    public String dailyRotationKey() {
        return LocalDate.now(zoneId()).toString();
    }

    public String weeklyRotationKey() {
        LocalDate date = LocalDate.now(zoneId());
        int week = date.get(WeekFields.ISO.weekOfWeekBasedYear());
        int year = date.get(WeekFields.ISO.weekBasedYear());
        return year + "-W" + week;
    }

    public CompletableFuture<List<QuestDefinition>> activeQuests(UUID uuid, String period) {
        boolean daily = period.equals("DAILY");
        Map<String, QuestDefinition> pool = daily ? dailyPool : weeklyPool;
        String rotationKey = daily ? dailyRotationKey() : weeklyRotationKey();
        int slots = plugin.quests().getInt("settings." + (daily ? "daily-slots" : "weekly-slots"), 3);

        return repository.loadRotationKey(uuid, period).thenCompose(existingKey -> {
            if (rotationKey.equals(existingKey)) {
                return repository.loadQuestIds(uuid, period).thenApply(csv -> resolveIds(csv, pool));
            }
            List<String> selected = randomSelection(pool.keySet(), slots, rotationKey + uuid);
            String csv = String.join(",", selected);
            return repository.saveRotation(uuid, period, rotationKey, csv).thenApply(ignored -> resolveIds(csv, pool));
        });
    }

    private List<QuestDefinition> resolveIds(String csv, Map<String, QuestDefinition> pool) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
                .map(pool::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<String> randomSelection(Set<String> keys, int amount, String seed) {
        List<String> pool = new ArrayList<>(keys);
        Collections.shuffle(pool, new Random(seed.hashCode()));
        return pool.subList(0, Math.min(amount, pool.size()));
    }

    public CompletableFuture<Map<String, long[]>> state(UUID uuid, String period) {
        return repository.loadQuestState(uuid, period);
    }

    public CompletableFuture<ProgressResult> addProgress(Player player, String eventType, String target) {
        UUID uuid = player.getUniqueId();
        return applyPeriod(player, "DAILY", eventType, target)
                .thenCompose(dailyResult -> applyPeriod(player, "WEEKLY", eventType, target)
                        .thenApply(weeklyResult -> dailyResult.merge(weeklyResult)));
    }

    private CompletableFuture<ProgressResult> applyPeriod(Player player, String period, String eventType, String target) {
        UUID uuid = player.getUniqueId();
        String rotationKey = period.equals("DAILY") ? dailyRotationKey() : weeklyRotationKey();

        return activeQuests(uuid, period).thenCompose(quests -> repository.loadQuestState(uuid, period).thenCompose(state -> {
            List<CompletableFuture<Void>> writes = new ArrayList<>();
            List<String> completedNow = new ArrayList<>();

            for (QuestDefinition quest : quests) {
                if (!quest.type().equals(eventType)) continue;
                if (!quest.target().equalsIgnoreCase("ANY") && !quest.target().equalsIgnoreCase(target)) continue;

                long[] current = state.getOrDefault(quest.id(), new long[]{0L, 0L});
                if (current[1] == 1L) continue;

                long newProgress = Math.min(quest.amount(), current[0] + 1);
                boolean justCompleted = newProgress >= quest.amount() && current[0] < quest.amount();
                writes.add(repository.upsertQuestState(uuid, period, quest.id(), newProgress, false, rotationKey));
                if (justCompleted) completedNow.add(quest.id());
            }

            if (writes.isEmpty()) return CompletableFuture.completedFuture(ProgressResult.empty());
            return CompletableFuture.allOf(writes.toArray(new CompletableFuture[0]))
                    .thenApply(ignored -> new ProgressResult(completedNow));
        }));
    }

    public CompletableFuture<ClaimResult> claim(Player player, String period, String questId) {
        UUID uuid = player.getUniqueId();
        Map<String, QuestDefinition> pool = period.equals("DAILY") ? dailyPool : weeklyPool;
        QuestDefinition quest = pool.get(questId);
        if (quest == null) return CompletableFuture.completedFuture(ClaimResult.failure("<red>Quest tidak ditemukan.</red>"));

        String rotationKey = period.equals("DAILY") ? dailyRotationKey() : weeklyRotationKey();

        return repository.loadQuestState(uuid, period).thenCompose(state -> {
            long[] current = state.getOrDefault(questId, new long[]{0L, 0L});
            if (current[1] == 1L) return CompletableFuture.completedFuture(ClaimResult.failure("<yellow>Sudah diklaim.</yellow>"));
            if (current[0] < quest.amount()) return CompletableFuture.completedFuture(ClaimResult.failure("<red>Progress belum selesai.</red>"));

            return repository.upsertQuestState(uuid, period, questId, current[0], true, rotationKey)
                    .thenApply(ignored -> ClaimResult.success(quest.rewardMoney(), quest.rewardJobXp()));
        });
    }

    public record ProgressResult(List<String> completedQuestIds) {
        public static ProgressResult empty() { return new ProgressResult(List.of()); }
        public ProgressResult merge(ProgressResult other) {
            List<String> combined = new ArrayList<>(completedQuestIds);
            combined.addAll(other.completedQuestIds);
            return new ProgressResult(combined);
        }
    }

    public record ClaimResult(boolean success, String message, double money, long jobXp) {
        public static ClaimResult success(double money, long jobXp) { return new ClaimResult(true, "", money, jobXp); }
        public static ClaimResult failure(String message) { return new ClaimResult(false, message, 0.0D, 0L); }
    }
}