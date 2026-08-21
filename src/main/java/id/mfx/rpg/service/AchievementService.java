package id.mfx.rpg.service;

import id.mfx.rpg.MFXRPG;
import id.mfx.rpg.model.AchievementDefinition;
import id.mfx.rpg.repository.ProgressRepository;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class AchievementService {

    private final MFXRPG plugin;
    private final ProgressRepository repository;
    private final Map<String, AchievementDefinition> achievements = new LinkedHashMap<>();

    private static final Set<String> ORES = Set.of(
            "COAL_ORE", "DEEPSLATE_COAL_ORE", "IRON_ORE", "DEEPSLATE_IRON_ORE",
            "GOLD_ORE", "DEEPSLATE_GOLD_ORE", "REDSTONE_ORE", "DEEPSLATE_REDSTONE_ORE",
            "LAPIS_ORE", "DEEPSLATE_LAPIS_ORE", "DIAMOND_ORE", "DEEPSLATE_DIAMOND_ORE",
            "EMERALD_ORE", "DEEPSLATE_EMERALD_ORE"
    );

    private static final Set<String> CROPS = Set.of(
            "WHEAT", "CARROTS", "POTATOES", "BEETROOTS", "NETHER_WART", "MELON", "PUMPKIN", "SUGAR_CANE"
    );

    public AchievementService(MFXRPG plugin, ProgressRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
        reload();
    }

    public void reload() {
        achievements.clear();
        ConfigurationSection section = plugin.achievements().getConfigurationSection("achievements");
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            ConfigurationSection achievement = section.getConfigurationSection(id);
            if (achievement == null) continue;
            achievements.put(id, new AchievementDefinition(
                    id,
                    achievement.getString("category", "GENERAL"),
                    achievement.getString("type", "BLOCK_BREAK"),
                    achievement.getString("target", "ANY"),
                    achievement.getLong("amount", 1),
                    achievement.getString("name", id),
                    achievement.getString("icon", "PAPER"),
                    achievement.getDouble("reward-money", 0.0D),
                    achievement.getInt("points", 0)
            ));
        }
    }

    public List<AchievementDefinition> achievements() {
        return List.copyOf(achievements.values());
    }

    public CompletableFuture<Map<String, long[]>> state(UUID uuid) {
        return repository.loadAchievements(uuid);
    }

    public CompletableFuture<List<String>> addProgress(Player player, String eventType, String target) {
        UUID uuid = player.getUniqueId();
        return repository.loadAchievements(uuid).thenCompose(state -> {
            List<CompletableFuture<Void>> writes = new ArrayList<>();
            List<String> unlockedNow = new ArrayList<>();

            for (AchievementDefinition achievement : achievements.values()) {
                if (!achievement.type().equals(eventType)) continue;
                if (!matchesTarget(achievement.target(), target)) continue;

                long[] current = state.getOrDefault(achievement.id(), new long[]{0L, 0L});
                if (current[1] == 1L) continue;

                long newProgress = Math.min(achievement.amount(), current[0] + 1);
                boolean justUnlocked = newProgress >= achievement.amount();
                writes.add(repository.upsertAchievement(uuid, achievement.id(), newProgress, justUnlocked));
                if (justUnlocked) unlockedNow.add(achievement.id());
            }

            if (writes.isEmpty()) return CompletableFuture.completedFuture(List.<String>of());
            return CompletableFuture.allOf(writes.toArray(new CompletableFuture[0])).thenApply(ignored -> unlockedNow);
        });
    }

    private boolean matchesTarget(String defined, String actual) {
        return switch (defined) {
            case "ANY" -> true;
            case "ANY_ORE" -> ORES.contains(actual);
            case "ANY_CROP" -> CROPS.contains(actual);
            default -> defined.equalsIgnoreCase(actual);
        };
    }

    public Optional<AchievementDefinition> byId(String id) {
        return Optional.ofNullable(achievements.get(id));
    }
}