package id.mfx.rpg.service;

import id.mfx.rpg.model.BattlePassProgress;
import id.mfx.rpg.model.BattlePassReward;
import id.mfx.rpg.model.BattlePassTier;
import id.mfx.rpg.repository.BattlePassRepository;
import id.mfx.rpg.storage.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class BattlePassService {

    public record ClaimResult(boolean success, String messageKey, int level, String track) {}

    private final JavaPlugin plugin;
    private final DatabaseManager database;
    private final BattlePassRepository repository;
    private String seasonId;
    private String displayName;
    private String premiumPermission;
    private int maxLevel;
    private int xpPerLevel;
    private final Map<Integer, BattlePassTier> tiers = new HashMap<>();

    public BattlePassService(JavaPlugin plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
        this.repository = new BattlePassRepository(database);
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "battlepass.yml");
        if (!file.exists()) plugin.saveResource("battlepass.yml", false);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection season = config.getConfigurationSection("season");
        seasonId = season == null ? "default" : season.getString("id", "default");
        displayName = color(season == null ? "Battle Pass" : season.getString("display-name", "Battle Pass"));
        premiumPermission = season == null ? "mfxrpg.battlepass.premium" : season.getString("premium-permission", "mfxrpg.battlepass.premium");
        maxLevel = Math.max(1, season == null ? 1 : season.getInt("max-level", 1));
        xpPerLevel = Math.max(1, season == null ? 1000 : season.getInt("xp-per-level", 1000));
        tiers.clear();
        ConfigurationSection rewards = config.getConfigurationSection("rewards");
        if (rewards == null) return;
        for (String rawLevel : rewards.getKeys(false)) {
            int level;
            try { level = Integer.parseInt(rawLevel); } catch (NumberFormatException ignored) { continue; }
            if (level < 1 || level > maxLevel) continue;
            ConfigurationSection tier = rewards.getConfigurationSection(rawLevel);
            if (tier == null) continue;
            tiers.put(level, new BattlePassTier(level, parseReward(tier.getConfigurationSection("free")), parseReward(tier.getConfigurationSection("premium"))));
        }
    }

    public String seasonId() { return seasonId; }
    public String displayName() { return displayName; }
    public int maxLevel() { return maxLevel; }
    public int xpPerLevel() { return xpPerLevel; }
    public List<BattlePassTier> tiers() { return tiers.values().stream().sorted(Comparator.comparingInt(BattlePassTier::level)).toList(); }
    public int levelForXp(int xp) { return Math.min(maxLevel, (xp / xpPerLevel) + 1); }
    public int requiredXpForLevel(int level) { return Math.max(0, (level - 1) * xpPerLevel); }

    public CompletableFuture<BattlePassProgress> progress(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try { return repository.getProgress(player.getUniqueId(), seasonId); }
            catch (SQLException exception) { throw new RuntimeException(exception); }
        }, database.executor());
    }

    public CompletableFuture<Integer> addXp(Player player, int amount) {
        if (amount <= 0) return CompletableFuture.completedFuture(0);
        return CompletableFuture.supplyAsync(() -> {
            try { return repository.addXp(player.getUniqueId(), seasonId, amount, System.currentTimeMillis()); }
            catch (SQLException exception) { throw new RuntimeException(exception); }
        }, database.executor());
    }

    public CompletableFuture<ClaimResult> claim(Player player, int level, String track) {
        if (!track.equals("free") && !track.equals("premium")) return CompletableFuture.completedFuture(new ClaimResult(false, "battlepass.locked", level, track));
        BattlePassTier tier = tiers.get(level);
        if (tier == null) return CompletableFuture.completedFuture(new ClaimResult(false, "battlepass.locked", level, track));
        if (track.equals("premium") && !player.hasPermission(premiumPermission)) {
            return CompletableFuture.completedFuture(new ClaimResult(false, "battlepass.premium-required", level, track));
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                return database.inTransaction(connection -> {
                    BattlePassProgress progress = repository.getProgress(player.getUniqueId(), seasonId);
                    if (levelForXp(progress.xp()) < level) return new ClaimResult(false, "battlepass.locked", level, track);
                    if (!repository.claimOnce(player.getUniqueId(), seasonId, level, track, System.currentTimeMillis())) {
                        return new ClaimResult(false, "battlepass.already-claimed", level, track);
                    }
                    return new ClaimResult(true, "battlepass.claimed", level, track);
                });
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        }, database.executor()).thenApply(result -> {
            if (result.success()) {
                BattlePassReward reward = track.equals("free") ? tier.freeReward() : tier.premiumReward();
                Bukkit.getScheduler().runTask(plugin, () -> dispatchReward(player, reward));
            }
            return result;
        });
    }

    private BattlePassReward parseReward(ConfigurationSection section) {
        if (section == null) return null;
        Material material;
        try { material = Material.valueOf(section.getString("material", "CHEST").toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { material = Material.CHEST; }
        return new BattlePassReward(color(section.getString("display-name", "Reward")), material, section.getStringList("commands"));
    }

    private void dispatchReward(Player player, BattlePassReward reward) {
        if (reward == null) return;
        for (String command : reward.commands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
        }
    }

    private String color(String text) { return text == null ? "" : text.replace('&', '§'); }
}