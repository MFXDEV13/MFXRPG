package id.mfx.rpg.repository;

import id.mfx.rpg.MFXRPG;
import id.mfx.rpg.model.PlayerJobData;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PlayerRepository {

    private final MFXRPG plugin;

    public PlayerRepository(MFXRPG plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<PlayerSnapshot> loadOrCreate(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String starterRank = plugin.rankService().starterRankId();
            try {
                ensureProfile(uuid);
                PlayerSnapshot snapshot = load(uuid);
                return snapshot == null ? PlayerSnapshot.fresh(uuid, starterRank) : snapshot;
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        }, plugin.database().executor());
    }

    public CompletableFuture<Void> save(PlayerSnapshot snapshot) {
        return CompletableFuture.runAsync(() -> {
            try {
                saveInternal(snapshot);
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        }, plugin.database().executor());
    }

    private void ensureProfile(UUID uuid) throws SQLException {
        long now = System.currentTimeMillis();
        try (PreparedStatement statement = plugin.database().connection().prepareStatement("""
                INSERT OR IGNORE INTO player_profiles (uuid, first_join, last_join)
                VALUES (?, ?, ?)
                """)) {
            statement.setString(1, uuid.toString());
            statement.setLong(2, now);
            statement.setLong(3, now);
            statement.executeUpdate();
        }

        try (PreparedStatement statement = plugin.database().connection().prepareStatement("""
                INSERT OR IGNORE INTO player_ranks (uuid, rank_id, xp, prestige)
                VALUES (?, ?, 0, 0)
                """)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, plugin.rankService().starterRankId());
            statement.executeUpdate();
        }

        try (PreparedStatement statement = plugin.database().connection().prepareStatement("""
                INSERT OR IGNORE INTO player_daily_rewards (uuid, streak, longest_streak, last_claim_date)
                VALUES (?, 0, 0, NULL)
                """)) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        }
    }

    private PlayerSnapshot load(UUID uuid) throws SQLException {
        String rankId;
        long rankXp;
        int prestige;
        int streak;
        int longest;
        LocalDate lastClaim;

        try (PreparedStatement statement = plugin.database().connection().prepareStatement("""
                SELECT r.rank_id, r.xp, r.prestige, d.streak, d.longest_streak, d.last_claim_date
                FROM player_ranks r
                JOIN player_daily_rewards d ON d.uuid = r.uuid
                WHERE r.uuid = ?
                """)) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                rankId = result.getString("rank_id");
                rankXp = result.getLong("xp");
                prestige = result.getInt("prestige");
                streak = result.getInt("streak");
                longest = result.getInt("longest_streak");
                String rawDate = result.getString("last_claim_date");
                lastClaim = rawDate == null ? null : LocalDate.parse(rawDate);
            }
        }

        Map<String, PlayerJobData> jobs = new HashMap<>();
        try (PreparedStatement statement = plugin.database().connection().prepareStatement("""
                SELECT job_id, level, xp, total_earnings FROM player_jobs WHERE uuid = ?
                """)) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String jobId = result.getString("job_id");
                    jobs.put(jobId, new PlayerJobData(
                            jobId,
                            result.getInt("level"),
                            result.getLong("xp"),
                            result.getDouble("total_earnings")
                    ));
                }
            }
        }

        return new PlayerSnapshot(uuid, rankId, rankXp, prestige, streak, longest, lastClaim, Map.copyOf(jobs));
    }

    private void saveInternal(PlayerSnapshot snapshot) throws SQLException {
        try (PreparedStatement statement = plugin.database().connection().prepareStatement("""
                UPDATE player_profiles SET last_join = ? WHERE uuid = ?
                """)) {
            statement.setLong(1, System.currentTimeMillis());
            statement.setString(2, snapshot.uuid().toString());
            statement.executeUpdate();
        }

        try (PreparedStatement statement = plugin.database().connection().prepareStatement("""
                UPDATE player_ranks SET rank_id = ?, xp = ?, prestige = ? WHERE uuid = ?
                """)) {
            statement.setString(1, snapshot.rankId());
            statement.setLong(2, snapshot.rankXp());
            statement.setInt(3, snapshot.prestige());
            statement.setString(4, snapshot.uuid().toString());
            statement.executeUpdate();
        }

        try (PreparedStatement statement = plugin.database().connection().prepareStatement("""
                UPDATE player_daily_rewards SET streak = ?, longest_streak = ?, last_claim_date = ? WHERE uuid = ?
                """)) {
            statement.setInt(1, snapshot.dailyStreak());
            statement.setInt(2, snapshot.longestDailyStreak());
            if (snapshot.lastDailyClaim() == null) {
                statement.setNull(3, java.sql.Types.VARCHAR);
            } else {
                statement.setString(3, snapshot.lastDailyClaim().toString());
            }
            statement.setString(4, snapshot.uuid().toString());
            statement.executeUpdate();
        }

        try (PreparedStatement delete = plugin.database().connection().prepareStatement("DELETE FROM player_jobs WHERE uuid = ?")) {
            delete.setString(1, snapshot.uuid().toString());
            delete.executeUpdate();
        }

        try (PreparedStatement insert = plugin.database().connection().prepareStatement("""
                INSERT INTO player_jobs (uuid, job_id, level, xp, total_earnings)
                VALUES (?, ?, ?, ?, ?)
                """)) {
            for (PlayerJobData job : snapshot.jobs().values()) {
                insert.setString(1, snapshot.uuid().toString());
                insert.setString(2, job.jobId());
                insert.setInt(3, job.level());
                insert.setLong(4, job.xp());
                insert.setDouble(5, job.totalEarnings());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }
}