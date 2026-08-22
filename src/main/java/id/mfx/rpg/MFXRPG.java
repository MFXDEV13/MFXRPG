    package id.mfx.rpg;

    import id.mfx.rpg.command.*;
    import id.mfx.rpg.gui.MenuListener;
    import id.mfx.rpg.hook.MfxRpgPlaceholderExpansion;
    import id.mfx.rpg.hook.VaultHook;
    import id.mfx.rpg.listener.JobActivityListener;
    import id.mfx.rpg.listener.PaketFourGuiListener;
    import id.mfx.rpg.listener.PlayerProfileListener;
    import id.mfx.rpg.listener.ProgressListener;
    import id.mfx.rpg.repository.PlayerRepository;
    import id.mfx.rpg.repository.ProgressRepository;
    import id.mfx.rpg.service.AchievementService;
    import id.mfx.rpg.service.BattlePassService;
    import id.mfx.rpg.service.CrateService;
    import id.mfx.rpg.service.DailyRewardService;
    import id.mfx.rpg.service.JobService;
    import id.mfx.rpg.service.LeaderboardService;
    import id.mfx.rpg.service.QuestService;
    import id.mfx.rpg.service.RankService;
    import id.mfx.rpg.service.RewardService;
    import id.mfx.rpg.storage.DatabaseManager;
    import id.mfx.rpg.util.TextService;
    import org.bukkit.command.PluginCommand;
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

        private CrateService crateService;
        private BattlePassService battlePassService;

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
            saveBundledFile("crates.yml");
            saveBundledFile("battlepass.yml");

            loadExternalConfigurations();

            if (!startDatabase()) {
                return;
            }

            vaultHook = new VaultHook(this);
            if (!vaultHook.setup() && getConfig().getBoolean("economy.require-vault", true)) {
                getLogger().severe("Vault/EssentialsX Economy diperlukan tetapi tidak ditemukan.");
                disableSelf();
                return;
            }

            initializeRepositories();
            initializeServices();
            registerListeners();
            registerCommands();
            registerPlaceholderApi();

            getLogger().info("MFXRPG Paket 4 aktif: Quest, Achievement, Profile, Leaderboard, Crate, dan Battle Pass.");
        }

        @Override
        public void onDisable() {
            if (database != null) {
                database.close();
            }
        }

        private boolean startDatabase() {
            try {
                String databaseFile = getConfig().getString("settings.database-file", "data.db");
                database = new DatabaseManager(this, databaseFile);
                database.connectAndMigrate();
                return true;
            } catch (SQLException exception) {
                getLogger().severe("Tidak dapat memulai SQLite: " + exception.getMessage());
                disableSelf();
                return false;
            }
        }

        private void initializeRepositories() {
            playerRepository = new PlayerRepository(this);
            progressRepository = new ProgressRepository(this);
        }

        private void initializeServices() {
            rewardService = new RewardService(this);
            rankService = new RankService(this, playerRepository);
            jobService = new JobService(this, playerRepository);
            dailyRewardService = new DailyRewardService(this, playerRepository);
            questService = new QuestService(this, progressRepository);
            achievementService = new AchievementService(this, progressRepository);
            leaderboardService = new LeaderboardService(this);

            crateService = new CrateService(this, database);
            battlePassService = new BattlePassService(this, database);
        }

        private void registerListeners() {
            var pluginManager = getServer().getPluginManager();

            pluginManager.registerEvents(new MenuListener(this), this);
            pluginManager.registerEvents(new PlayerProfileListener(this), this);
            pluginManager.registerEvents(new JobActivityListener(this), this);
            pluginManager.registerEvents(new ProgressListener(this), this);
            pluginManager.registerEvents(new PaketFourGuiListener(), this);
        }

        private void registerCommands() {
            register("rpg", new RpgCommand(this));
            register("rank", new RankCommand(this));
            register("rankup", new RankupCommand(this));
            register("prestige", new PrestigeCommand(this));
            register("jobs", new JobsCommand(this));
            register("daily", new DailyCommand(this));
            register("quests", new QuestsCommand(this));
            register("achievements", new AchievementsCommand(this));
            register("profile", new ProfileCommand(this));
            register("top", new TopCommand(this));
            register("mfxrpg", new AdminCommand(this));

            CrateCommand crateCommand = new CrateCommand(crateService);
            register("crate", crateCommand, crateCommand);

            register("battlepass", new BattlePassCommand(battlePassService));
        }

        private void registerPlaceholderApi() {
            if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
                return;
            }

            new MfxRpgPlaceholderExpansion(this).register();
            getLogger().info("Hook PlaceholderAPI berhasil diaktifkan.");
        }

        private void register(String name, org.bukkit.command.CommandExecutor executor) {
            PluginCommand command = getCommand(name);

            if (command == null) {
                getLogger().warning("Command '" + name + "' tidak ditemukan di plugin.yml.");
                return;
            }

            command.setExecutor(executor);
        }

        private void register(
                String name,
                org.bukkit.command.CommandExecutor executor,
                org.bukkit.command.TabCompleter completer
        ) {
            PluginCommand command = getCommand(name);

            if (command == null) {
                getLogger().warning("Command '" + name + "' tidak ditemukan di plugin.yml.");
                return;
            }

            command.setExecutor(executor);
            command.setTabCompleter(completer);
        }

        public void reloadAllConfigurations() {
            reloadConfig();
            loadExternalConfigurations();

            if (rankService != null) {
                rankService.reload();
            }

            if (jobService != null) {
                jobService.reload();
            }

            if (questService != null) {
                questService.reload();
            }

            if (achievementService != null) {
                achievementService.reload();
            }

            if (crateService != null) {
                crateService.reload();
            }

            if (battlePassService != null) {
                battlePassService.reload();
            }
        }

        public TextService text() {
            return text;
        }

        public DatabaseManager database() {
            return database;
        }

        public VaultHook vaultHook() {
            return vaultHook;
        }

        public PlayerRepository playerRepository() {
            return playerRepository;
        }

        public ProgressRepository progressRepository() {
            return progressRepository;
        }

        public RewardService rewardService() {
            return rewardService;
        }

        public RankService rankService() {
            return rankService;
        }

        public JobService jobService() {
            return jobService;
        }

        public DailyRewardService dailyRewardService() {
            return dailyRewardService;
        }

        public QuestService questService() {
            return questService;
        }

        public AchievementService achievementService() {
            return achievementService;
        }

        public LeaderboardService leaderboardService() {
            return leaderboardService;
        }

        public CrateService crateService() {
            return crateService;
        }

        public BattlePassService battlePassService() {
            return battlePassService;
        }

        public FileConfiguration theme() {
            return theme;
        }

        public FileConfiguration ranks() {
            return ranks;
        }

        public FileConfiguration jobs() {
            return jobs;
        }

        public FileConfiguration dailyRewards() {
            return dailyRewards;
        }

        public FileConfiguration quests() {
            return quests;
        }

        public FileConfiguration achievements() {
            return achievements;
        }

        public FileConfiguration leaderboards() {
            return leaderboards;
        }

        public String message(String path) {
            String raw = messages.getString(path, "<red>Missing message: " + path + "</red>");
            return raw.replace("<prefix>", messages.getString("prefix", ""));
        }

        private void saveBundledFile(String name) {
            File file = new File(getDataFolder(), name);

            if (!file.exists()) {
                saveResource(name, false);
            }
        }

        private void loadExternalConfigurations() {
            messages = loadYaml("messages.yml");
            theme = loadYaml("theme.yml");
            ranks = loadYaml("ranks.yml");
            jobs = loadYaml("jobs.yml");
            dailyRewards = loadYaml("daily-rewards.yml");
            quests = loadYaml("quests.yml");
            achievements = loadYaml("achievements.yml");
            leaderboards = loadYaml("leaderboards.yml");
        }

        private FileConfiguration loadYaml(String fileName) {
            return YamlConfiguration.loadConfiguration(new File(getDataFolder(), fileName));
        }

        private void disableSelf() {
            getServer().getPluginManager().disablePlugin(this);
        }
    }