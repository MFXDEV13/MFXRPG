package id.mfx.rpg.service;

import id.mfx.rpg.MFXRPG;
import id.mfx.rpg.model.RankDefinition;
import id.mfx.rpg.repository.PlayerRepository;
import id.mfx.rpg.repository.PlayerSnapshot;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class RankService {

    private final MFXRPG plugin;
    private final PlayerRepository repository;
    private final Map<String, RankDefinition> ranks = new LinkedHashMap<>();

    public RankService(MFXRPG plugin, PlayerRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
        reload();
    }

    public void reload() {
        ranks.clear();
        ConfigurationSection section = plugin.ranks().getConfigurationSection("ranks");
        if (section == null) {
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection rank = section.getConfigurationSection(id);
            if (rank == null) {
                continue;
            }
            ranks.put(id, new RankDefinition(
                    id,
                    rank.getInt("order"),
                    rank.getString("display-name", id),
                    rank.getString("icon", "STONE"),
                    rank.getLong("required-xp"),
                    rank.getDouble("price"),
                    rank.getStringList("benefits")
            ));
        }
    }

    public String starterRankId() {
        return plugin.ranks().getString("settings.starter-rank", "novice");
    }

    public List<RankDefinition> orderedRanks() {
        return ranks.values().stream().sorted(Comparator.comparingInt(RankDefinition::order)).toList();
    }

    public Optional<RankDefinition> rank(String id) {
        return Optional.ofNullable(ranks.get(id));
    }

    public Optional<RankDefinition> nextRank(PlayerSnapshot snapshot) {
        return orderedRanks().stream().filter(rank -> rank.order() > rank(snapshot.rankId()).map(RankDefinition::order).orElse(0)).findFirst();
    }

    public CompletableFuture<PlayerSnapshot> load(UUID uuid) {
        return repository.loadOrCreate(uuid);
    }

    public CompletableFuture<RankupResult> rankup(Player player) {
        return load(player.getUniqueId()).thenCompose(snapshot -> {
            Optional<RankDefinition> targetOptional = nextRank(snapshot);
            if (targetOptional.isEmpty()) {
                return CompletableFuture.completedFuture(RankupResult.failure("<red>Kamu sudah berada di rank maksimum.</red>"));
            }

            RankDefinition target = targetOptional.get();
            if (snapshot.rankXp() < target.requiredXp()) {
                return CompletableFuture.completedFuture(RankupResult.failure("<red>XP rank belum cukup.</red>"));
            }

            if (!plugin.rewardService().withdraw(player, target.price())) {
                return CompletableFuture.completedFuture(RankupResult.failure("<red>Coins kamu tidak cukup untuk rank-up.</red>"));
            }

            PlayerSnapshot updated = new PlayerSnapshot(
                    snapshot.uuid(), target.id(), snapshot.rankXp(), snapshot.prestige(),
                    snapshot.dailyStreak(), snapshot.longestDailyStreak(), snapshot.lastDailyClaim(), snapshot.jobs()
            );
            return repository.save(updated).thenApply(ignored -> RankupResult.success(target));
        });
    }

    public CompletableFuture<PrestigeResult> prestige(Player player) {
        return load(player.getUniqueId()).thenCompose(snapshot -> {
            if (!plugin.ranks().getBoolean("settings.prestige.enabled", true)) {
                return CompletableFuture.completedFuture(PrestigeResult.failure("<red>Prestige sedang dinonaktifkan.</red>"));
            }

            String required = plugin.ranks().getString("settings.prestige.required-rank", "champion");
            if (!snapshot.rankId().equalsIgnoreCase(required)) {
                return CompletableFuture.completedFuture(PrestigeResult.failure("<red>Kamu harus mencapai rank terakhir terlebih dahulu.</red>"));
            }

            String resetRank = plugin.ranks().getString("settings.prestige.reset-rank", starterRankId());
            PlayerSnapshot updated = new PlayerSnapshot(
                    snapshot.uuid(), resetRank, 0L, snapshot.prestige() + 1,
                    snapshot.dailyStreak(), snapshot.longestDailyStreak(), snapshot.lastDailyClaim(), snapshot.jobs()
            );
            return repository.save(updated).thenApply(ignored -> PrestigeResult.success(updated.prestige()));
        });
    }

    public record RankupResult(boolean success, String message, RankDefinition rank) {
        public static RankupResult success(RankDefinition rank) {
            return new RankupResult(true, "", rank);
        }

        public static RankupResult failure(String message) {
            return new RankupResult(false, message, null);
        }
    }

    public record PrestigeResult(boolean success, String message, int prestige) {
        public static PrestigeResult success(int prestige) {
            return new PrestigeResult(true, "", prestige);
        }

        public static PrestigeResult failure(String message) {
            return new PrestigeResult(false, message, 0);
        }
    }
}