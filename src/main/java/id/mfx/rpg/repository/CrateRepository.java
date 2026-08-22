package id.mfx.rpg.repository;

import id.mfx.rpg.storage.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CrateRepository {

    private final DatabaseManager database;

    public CrateRepository(DatabaseManager database) {
        this.database = database;
    }

    public int getKeys(UUID uuid, String crateId) throws SQLException {
        String sql = "SELECT amount FROM player_crate_keys WHERE uuid = ? AND crate_id = ?";
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, crateId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt("amount") : 0;
            }
        }
    }

    public void addKeys(UUID uuid, String crateId, int amount) throws SQLException {
        if (amount <= 0) throw new IllegalArgumentException("amount harus positif");
        String sql = """
                INSERT INTO player_crate_keys(uuid, crate_id, amount) VALUES (?, ?, ?)
                ON CONFLICT(uuid, crate_id) DO UPDATE SET amount = amount + excluded.amount
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, crateId);
            statement.setInt(3, amount);
            statement.executeUpdate();
        }
    }

    public boolean consumeKey(UUID uuid, String crateId) throws SQLException {
        String sql = """
                UPDATE player_crate_keys
                SET amount = amount - 1
                WHERE uuid = ? AND crate_id = ? AND amount >= 1
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, crateId);
            return statement.executeUpdate() == 1;
        }
    }

    public int getPityMisses(UUID uuid, String crateId) throws SQLException {
        String sql = "SELECT misses FROM player_crate_pity WHERE uuid = ? AND crate_id = ?";
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, crateId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt("misses") : 0;
            }
        }
    }

    public void setPityMisses(UUID uuid, String crateId, int misses) throws SQLException {
        String sql = """
                INSERT INTO player_crate_pity(uuid, crate_id, misses) VALUES (?, ?, ?)
                ON CONFLICT(uuid, crate_id) DO UPDATE SET misses = excluded.misses
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, crateId);
            statement.setInt(3, Math.max(0, misses));
            statement.executeUpdate();
        }
    }

    public void addHistory(UUID uuid, String crateId, String rewardId, String rarity, long openedAt) throws SQLException {
        String sql = """
                INSERT INTO crate_open_history(uuid, crate_id, reward_id, rarity, opened_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, crateId);
            statement.setString(3, rewardId);
            statement.setString(4, rarity);
            statement.setLong(5, openedAt);
            statement.executeUpdate();
        }
    }

    public List<String> recentHistory(UUID uuid, int limit) throws SQLException {
        String sql = """
                SELECT crate_id, reward_id, rarity, opened_at FROM crate_open_history
                WHERE uuid = ? ORDER BY opened_at DESC LIMIT ?
                """;
        List<String> entries = new ArrayList<>();
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setInt(2, Math.max(1, limit));
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    entries.add(result.getString("crate_id") + ":" + result.getString("reward_id")
                            + ":" + result.getString("rarity") + ":" + result.getLong("opened_at"));
                }
            }
        }
        return entries;
    }
}