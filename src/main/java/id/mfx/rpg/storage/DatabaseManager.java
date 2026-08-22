package id.mfx.rpg.storage;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DatabaseManager {

    private final JavaPlugin plugin;
    private final ExecutorService executor;
    private final String url;

    private Connection connection;

    public DatabaseManager(JavaPlugin plugin, String databaseFileName) {
        this.plugin = plugin;

        this.executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "MFXRPG-SQLite");
            thread.setDaemon(true);
            return thread;
        });

        this.url = "jdbc:sqlite:"
                + new File(plugin.getDataFolder(), databaseFileName).getAbsolutePath();
    }

    public void connectAndMigrate() throws SQLException {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new SQLException("Gagal membuat folder data MFXRPG.");
        }

        connection = DriverManager.getConnection(url);

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA foreign_keys=ON");
            statement.execute("PRAGMA busy_timeout=5000");

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS player_profiles (
                        uuid TEXT PRIMARY KEY NOT NULL,
                        first_join INTEGER NOT NULL,
                        last_join INTEGER NOT NULL,
                        selected_title TEXT NOT NULL DEFAULT '',
                        settings_json TEXT NOT NULL DEFAULT '{}'
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS player_ranks (
                        uuid TEXT PRIMARY KEY NOT NULL,
                        rank_id TEXT NOT NULL DEFAULT 'novice',
                        xp INTEGER NOT NULL DEFAULT 0,
                        prestige INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (uuid) REFERENCES player_profiles(uuid) ON DELETE CASCADE
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS player_jobs (
                        uuid TEXT NOT NULL,
                        job_id TEXT NOT NULL,
                        level INTEGER NOT NULL DEFAULT 1,
                        xp INTEGER NOT NULL DEFAULT 0,
                        total_earnings REAL NOT NULL DEFAULT 0,
                        PRIMARY KEY (uuid, job_id),
                        FOREIGN KEY (uuid) REFERENCES player_profiles(uuid) ON DELETE CASCADE
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS player_daily_rewards (
                        uuid TEXT PRIMARY KEY NOT NULL,
                        streak INTEGER NOT NULL DEFAULT 0,
                        longest_streak INTEGER NOT NULL DEFAULT 0,
                        last_claim_date TEXT,
                        FOREIGN KEY (uuid) REFERENCES player_profiles(uuid) ON DELETE CASCADE
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS player_crate_pity (
                        uuid TEXT NOT NULL,
                        crate_id TEXT NOT NULL,
                        misses INTEGER NOT NULL DEFAULT 0 CHECK (misses >= 0),
                        PRIMARY KEY (uuid, crate_id),
                        FOREIGN KEY (uuid) REFERENCES player_profiles(uuid) ON DELETE CASCADE
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS crate_open_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid TEXT NOT NULL,
                        crate_id TEXT NOT NULL,
                        reward_id TEXT NOT NULL,
                        rarity TEXT NOT NULL,
                        opened_at INTEGER NOT NULL,
                        FOREIGN KEY (uuid) REFERENCES player_profiles(uuid) ON DELETE CASCADE
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS player_crate_keys (
                        uuid TEXT NOT NULL,
                        crate_id TEXT NOT NULL,
                        amount INTEGER NOT NULL DEFAULT 0 CHECK (amount >= 0),
                        PRIMARY KEY (uuid, crate_id),
                        FOREIGN KEY (uuid) REFERENCES player_profiles(uuid) ON DELETE CASCADE
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS player_quest_state (
                        uuid TEXT NOT NULL,
                        period TEXT NOT NULL,
                        quest_id TEXT NOT NULL,
                        progress INTEGER NOT NULL DEFAULT 0,
                        claimed INTEGER NOT NULL DEFAULT 0,
                        rotation_key TEXT NOT NULL,
                        PRIMARY KEY (uuid, period, quest_id),
                        FOREIGN KEY (uuid) REFERENCES player_profiles(uuid) ON DELETE CASCADE
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS player_quest_rotation (
                        uuid TEXT NOT NULL,
                        period TEXT NOT NULL,
                        rotation_key TEXT NOT NULL,
                        quest_ids TEXT NOT NULL,
                        PRIMARY KEY (uuid, period),
                        FOREIGN KEY (uuid) REFERENCES player_profiles(uuid) ON DELETE CASCADE
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS player_achievements (
                        uuid TEXT NOT NULL,
                        achievement_id TEXT NOT NULL,
                        progress INTEGER NOT NULL DEFAULT 0,
                        unlocked INTEGER NOT NULL DEFAULT 0,
                        unlocked_at INTEGER,
                        PRIMARY KEY (uuid, achievement_id),
                        FOREIGN KEY (uuid) REFERENCES player_profiles(uuid) ON DELETE CASCADE
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS battlepass_progress (
                        uuid TEXT NOT NULL,
                        season_id TEXT NOT NULL,
                        xp INTEGER NOT NULL DEFAULT 0 CHECK (xp >= 0),
                        updated_at INTEGER NOT NULL,
                        PRIMARY KEY (uuid, season_id),
                        FOREIGN KEY (uuid) REFERENCES player_profiles(uuid) ON DELETE CASCADE
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS battlepass_claims (
                        uuid TEXT NOT NULL,
                        season_id TEXT NOT NULL,
                        level INTEGER NOT NULL CHECK (level > 0),
                        track TEXT NOT NULL CHECK (track IN ('free', 'premium')),
                        claimed_at INTEGER NOT NULL,
                        PRIMARY KEY (uuid, season_id, level, track),
                        FOREIGN KEY (uuid) REFERENCES player_profiles(uuid) ON DELETE CASCADE
                    )
                    """);

            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_crate_open_history_uuid_opened_at
                    ON crate_open_history(uuid, opened_at DESC)
                    """);

            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_battlepass_progress_uuid_season
                    ON battlepass_progress(uuid, season_id)
                    """);
        }
    }

    public ExecutorService executor() {
        return executor;
    }

    public Connection connection() {
        if (connection == null) {
            throw new IllegalStateException("Database belum tersambung.");
        }

        return connection;
    }

    public <T> T inTransaction(SqlWork<T> work) throws SQLException {
        Connection current = connection();
        boolean previousAutoCommit = current.getAutoCommit();

        current.setAutoCommit(false);

        try {
            T result = work.execute(current);
            current.commit();
            return result;
        } catch (SQLException | RuntimeException exception) {
            try {
                current.rollback();
            } catch (SQLException rollbackException) {
                exception.addSuppressed(rollbackException);
            }

            throw exception;
        } finally {
            current.setAutoCommit(previousAutoCommit);
        }
    }

    @FunctionalInterface
    public interface SqlWork<T> {
        T execute(Connection connection) throws SQLException;
    }

    public void close() {
        executor.shutdown();

        if (connection == null) {
            return;
        }

        try {
            connection.close();
        } catch (SQLException exception) {
            plugin.getLogger().warning(
                    "Gagal menutup SQLite: " + exception.getMessage()
            );
        } finally {
            connection = null;
        }
    }
}