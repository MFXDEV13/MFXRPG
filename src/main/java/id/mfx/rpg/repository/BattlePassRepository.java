package id.mfx.rpg.repository;

import id.mfx.rpg.model.BattlePassProgress;
import id.mfx.rpg.storage.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class BattlePassRepository {

    private final DatabaseManager database;

    public BattlePassRepository(DatabaseManager database) {
        this.database = database;
    }

    public BattlePassProgress getProgress(UUID uuid, String seasonId) throws SQLException {
        String sql = "SELECT xp, updated_at FROM battlepass_progress WHERE uuid = ? AND season_id = ?";
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, seasonId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) return new BattlePassProgress(seasonId, result.getInt("xp"), result.getLong("updated_at"));
            }
        }
        return new BattlePassProgress(seasonId, 0, 0L);
    }

    public int addXp(UUID uuid, String seasonId, int amount, long now) throws SQLException {
        String sql = """
                INSERT INTO battlepass_progress(uuid, season_id, xp, updated_at) VALUES (?, ?, ?, ?)
                ON CONFLICT(uuid, season_id) DO UPDATE SET
                    xp = battlepass_progress.xp + excluded.xp,
                    updated_at = excluded.updated_at
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, seasonId);
            statement.setInt(3, Math.max(0, amount));
            statement.setLong(4, now);
            statement.executeUpdate();
        }
        return getProgress(uuid, seasonId).xp();
    }

    public boolean isClaimed(UUID uuid, String seasonId, int level, String track) throws SQLException {
        String sql = """
                SELECT 1 FROM battlepass_claims
                WHERE uuid = ? AND season_id = ? AND level = ? AND track = ?
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, seasonId);
            statement.setInt(3, level);
            statement.setString(4, track);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    public boolean claimOnce(UUID uuid, String seasonId, int level, String track, long now) throws SQLException {
        String sql = """
                INSERT OR IGNORE INTO battlepass_claims(uuid, season_id, level, track, claimed_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, seasonId);
            statement.setInt(3, level);
            statement.setString(4, track);
            statement.setLong(5, now);
            return statement.executeUpdate() == 1;
        }
    }
}