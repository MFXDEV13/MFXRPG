package id.mfx.rpg.service;

import id.mfx.rpg.model.CrateDefinition;
import id.mfx.rpg.model.CrateReward;
import id.mfx.rpg.repository.CrateRepository;
import id.mfx.rpg.storage.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class CrateService {

    public record OpenResult(boolean success, String messageKey, CrateDefinition crate, CrateReward reward, boolean pityTriggered) {}

    private final JavaPlugin plugin;
    private final DatabaseManager database;
    private final CrateRepository repository;
    private final Map<String, CrateDefinition> crates = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final SplittableRandom random = new SplittableRandom();

    public CrateService(JavaPlugin plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
        this.repository = new CrateRepository(database);
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "crates.yml");
        if (!file.exists()) plugin.saveResource("crates.yml", false);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        crates.clear();
        ConfigurationSection root = config.getConfigurationSection("crates");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) continue;
            Map<String, CrateReward> rewards = new HashMap<>();
            ConfigurationSection rewardSection = section.getConfigurationSection("rewards");
            if (rewardSection != null) {
                for (String rewardId : rewardSection.getKeys(false)) {
                    ConfigurationSection reward = rewardSection.getConfigurationSection(rewardId);
                    if (reward == null) continue;
                    int weight = Math.max(0, reward.getInt("weight", 0));
                    if (weight == 0) continue;
                    rewards.put(rewardId, new CrateReward(
                            rewardId,
                            color(reward.getString("display-name", rewardId)),
                            reward.getString("rarity", "common").toLowerCase(Locale.ROOT),
                            weight,
                            material(reward.getString("material", "CHEST"), Material.CHEST),
                            reward.getStringList("commands")
                    ));
                }
            }
            ConfigurationSection pity = section.getConfigurationSection("pity");
            crates.put(id.toLowerCase(Locale.ROOT), new CrateDefinition(
                    id.toLowerCase(Locale.ROOT),
                    color(section.getString("display-name", id)),
                    material(section.getString("icon", "CHEST"), Material.CHEST),
                    section.getString("key-name", id + " Key"),
                    Math.max(0, section.getInt("open-cooldown-seconds", 0)),
                    pity != null && pity.getBoolean("enabled", false),
                    pity == null ? List.of() : pity.getStringList("target-rarities").stream()
                            .map(value -> value.toLowerCase(Locale.ROOT)).toList(),
                    pity == null ? 0 : Math.max(1, pity.getInt("guarantee-after-misses", 1)),
                    Map.copyOf(rewards)
            ));
        }
    }

    public Collection<CrateDefinition> crates() {
        return crates.values().stream().sorted(Comparator.comparing(CrateDefinition::id)).toList();
    }

    public CrateDefinition getCrate(String id) {
        return crates.get(id.toLowerCase(Locale.ROOT));
    }

    public CompletableFuture<Integer> getKeys(UUID uuid, String crateId) {
        return CompletableFuture.supplyAsync(() -> {
            try { return repository.getKeys(uuid, crateId); }
            catch (SQLException exception) { throw new RuntimeException(exception); }
        }, database.executor());
    }

    public CompletableFuture<Void> giveKeys(UUID uuid, String crateId, int amount) {
        return CompletableFuture.runAsync(() -> {
            if (getCrate(crateId) == null) throw new IllegalArgumentException("Crate tidak ditemukan");
            try { repository.addKeys(uuid, crateId, amount); }
            catch (SQLException exception) { throw new RuntimeException(exception); }
        }, database.executor());
    }

    public CompletableFuture<OpenResult> open(Player player, String crateId) {
        CrateDefinition crate = getCrate(crateId);
        if (crate == null) return CompletableFuture.completedFuture(new OpenResult(false, "crate.unknown", null, null, false));
        if (crate.rewards().isEmpty()) return CompletableFuture.completedFuture(new OpenResult(false, "crate.unknown", crate, null, false));

        long now = System.currentTimeMillis();
        Long nextOpen = cooldowns.get(player.getUniqueId());
        if (nextOpen != null && nextOpen > now) {
            return CompletableFuture.completedFuture(new OpenResult(false, "crate.cooldown", crate, null, false));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                return database.inTransaction(connection -> {
                    if (!repository.consumeKey(player.getUniqueId(), crate.id())) {
                        return new OpenResult(false, "crate.no-key", crate, null, false);
                    }
                    int misses = crate.pityEnabled() ? repository.getPityMisses(player.getUniqueId(), crate.id()) : 0;
                    boolean forcePity = crate.pityEnabled() && misses >= crate.pityGuaranteeAfterMisses() - 1;
                    CrateReward reward = roll(crate, forcePity);
                    if (reward == null) throw new SQLException("Tidak ada reward valid untuk crate " + crate.id());
                    boolean target = crate.pityTargetRarities().contains(reward.rarity());
                    if (crate.pityEnabled()) repository.setPityMisses(player.getUniqueId(), crate.id(), target ? 0 : misses + 1);
                    repository.addHistory(player.getUniqueId(), crate.id(), reward.id(), reward.rarity(), now);
                    return new OpenResult(true, "crate.opened", crate, reward, forcePity);
                });
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        }, database.executor()).thenApply(result -> {
            if (result.success()) {
                cooldowns.put(player.getUniqueId(), now + crate.cooldownSeconds() * 1000L);
                Bukkit.getScheduler().runTask(plugin, () -> giveReward(player, result.reward()));
            }
            return result;
        });
    }

    public CompletableFuture<List<String>> history(UUID uuid, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try { return repository.recentHistory(uuid, limit); }
            catch (SQLException exception) { throw new RuntimeException(exception); }
        }, database.executor());
    }

    private CrateReward roll(CrateDefinition crate, boolean forcePity) {
        List<CrateReward> pool = new ArrayList<>(crate.rewards().values());
        if (forcePity) pool.removeIf(reward -> !crate.pityTargetRarities().contains(reward.rarity()));
        if (pool.isEmpty()) return null;
        int total = pool.stream().mapToInt(CrateReward::weight).sum();
        int hit = random.nextInt(total);
        for (CrateReward reward : pool) {
            hit -= reward.weight();
            if (hit < 0) return reward;
        }
        return pool.getLast();
    }

    private void giveReward(Player player, CrateReward reward) {
        for (String command : reward.commands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
        }
    }

    private Material material(String raw, Material fallback) {
        try { return Material.valueOf(Objects.requireNonNullElse(raw, fallback.name()).toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return fallback; }
    }

    private String color(String text) {
        return text == null ? "" : text.replace('&', '§');
    }
}