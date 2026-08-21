package id.mfx.rpg;

import id.mfx.rpg.command.*;
import id.mfx.rpg.gui.MenuListener;
import id.mfx.rpg.hook.VaultHook;
import id.mfx.rpg.listener.JobActivityListener;
import id.mfx.rpg.listener.PlayerProfileListener;
import id.mfx.rpg.listener.ProgressListener;
import id.mfx.rpg.repository.PlayerRepository;
import id.mfx.rpg.repository.ProgressRepository;
import id.mfx.rpg.service.*;
import id.mfx.rpg.storage.DatabaseManager;
import id.mfx.rpg.util.TextService;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;

public final class MFXRPG extends JavaPlugin {

    private final TextService text = new TextService();
    private FileConfiguration messages;
    private FileConfiguration theme;
    private FileConfiguration ranks;
    private FileConfiguration jobs;
    private FileConfiguration dailyRewards;
    private FileConfiguration quests;
    private FileConfiguration achievements;
    private FileConfiguration leaderboards;

    private DatabaseManager database;
    private VaultHook vaultHook;
    private PlayerRepository playerRepository;
    private ProgressRepository progressRepository;
    private RewardService rewardService;
    private RankService rankService;
    private JobService jobService;
    private DailyRewardService dailyRewardService;
    private QuestService questService;
    private AchievementService achievementService;
    private LeaderboardService leaderboardService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveBundledFile("messages.yml");
        saveBundledFile("theme.yml");
        saveBundledFile("ranks.yml");
        saveBundledFile("jobs.yml");
        saveBundledFile("daily-rewards.yml");
        saveBundledFile("quests.yml");
        saveBundledFile("achievements.yml");
        saveBundledFile("leaderboards.yml");
        loadExternalConfigurations();

        try {
            database = new DatabaseManager(this, getConfig().getString("settings.database-file", "data.db"));
            database.connectAndMigrate();
        } catch (SQLException exception) {
            getLogger().severe("Tidak dapat memulai SQLite: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        vaultHook = new VaultHook(this);
        if (!vaultHook.setup() && getConfig().getBoolean("economy.require-vault", true)) {
            getLogger().severe("Vault/EssentialsX Economy diperlukan tetapi tidak ditemukan.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        playerRepository = new PlayerRepository(this);
        progressRepository = new ProgressRepository(this);
        rewardService = new RewardService(this);
        rankService = new RankService(this, playerRepository);
        jobService = new JobService(this, playerRepository);
        dailyRewardService = new DailyRewardService(this, playerRepository);
        questService = new QuestService(this, progressRepository);
        achievementService = new AchievementService(this, progressRepository);
        leaderboardService = new LeaderboardService(this);

        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerProfileListener(this), this);
        getServer().getPluginManager().registerEvents(new JobActivityListener(this), this);
        getServer().getPluginManager().registerEvents(new ProgressListener(this), this);

        getCommand("rpg").setExecutor(new RpgCommand(this));
        getCommand("rank").setExecutor(new RankCommand(this));
        getCommand("rankup").setExecutor(new RankupCommand(this));
        getCommand("prestige").setExecutor(new PrestigeCommand(this));
        getCommand("jobs").setExecutor(new JobsCommand(this));
        getCommand("daily").setExecutor(new DailyCommand(this));
        getCommand("quests").setExecutor(new QuestsCommand(this));
        getCommand("achievements").setExecutor(new AchievementsCommand(this));
        getCommand("profile").setExecutor(new ProfileCommand(this));
        getCommand("top").setExecutor(new TopCommand(this));
        getCommand("mfxrpg").setExecutor(new AdminCommand(this));

        getLogger().info("MFXRPG Paket 3 aktif: Quest, Achievement, Profile, Leaderboard.");
    }

    @Override
    public void onDisable() {
        if (database != null) database.close();
    }

    public void reloadAllConfigurations() {
        reloadConfig();
        loadExternalConfigurations();
        if (rankService != null) rankService.reload();
        if (jobService != null) jobService.reload();
        if (questService != null) questService.reload();
        if (achievementService != null) achievementService.reload();
    }

    public TextService text() { return text; }
    public DatabaseManager database() { return database; }
    public VaultHook vaultHook() { return vaultHook; }
    public PlayerRepository playerRepository() { return playerRepository; }
    public ProgressRepository progressRepository() { return progressRepository; }
    public RewardService rewardService() { return rewardService; }
    public RankService rankService() { return rankService; }
    public JobService jobService() { return jobService; }
    public DailyRewardService dailyRewardService() { return dailyRewardService; }
    public QuestService questService() { return questService; }
    public AchievementService achievementService() { return achievementService; }
    public LeaderboardService leaderboardService() { return leaderboardService; }

    public FileConfiguration theme() { return theme; }
    public FileConfiguration ranks() { return ranks; }
    public FileConfiguration jobs() { return jobs; }
    public FileConfiguration dailyRewards() { return dailyRewards; }
    public FileConfiguration quests() { return quests; }
    public FileConfiguration achievements() { return achievements; }
    public FileConfiguration leaderboards() { return leaderboards; }

    public String message(String path) {
        String raw = messages.getString(path, "<red>Missing message: " + path + "</red>");
        return raw.replace("<prefix>", messages.getString("prefix", ""));
    }

    private void saveBundledFile(String name) {
        File file = new File(getDataFolder(), name);
        if (!file.exists()) saveResource(name, false);
    }

    private void loadExternalConfigurations() {
        messages = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml"));
        theme = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "theme.yml"));
        ranks = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "ranks.yml"));
        jobs = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "jobs.yml"));
        dailyRewards = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "daily-rewards.yml"));
        quests = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "quests.yml"));
        achievements = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "achievements.yml"));
        leaderboards = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "leaderboards.yml"));
    }
}