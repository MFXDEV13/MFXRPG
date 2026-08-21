package id.mfx.rpg.service;

import id.mfx.rpg.MFXRPG;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class LeaderboardService {

    private final MFXRPG plugin;

    public LeaderboardService(MFXRPG plugin) {
        this.plugin = plugin;
    }

    public record Entry(String name, double value) {
    }

    public CompletableFuture<List<Entry>> topRank(int limit) {
        return CompletableFuture.supplyAsync(() -> queryTop("""
                SELECT uuid, (prestige * 1000000 + xp) AS score FROM player_ranks
                ORDER BY score DESC LIMIT ?
                """, limit), plugin.database().executor());
    }

    public CompletableFuture<List<Entry>> topAchievementPoints(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<Entry> result = new ArrayList<>();
            String sql = """
                    SELECT uuid, SUM(unlocked) AS score FROM player_achievements
                    GROUP BY uuid ORDER BY score DESC LIMIT ?
                    """;
            try (PreparedStatement statement = plugin.database().connection().prepareStatement(sql)) {
                statement.setInt(1, limit);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        result.add(new Entry(resolveName(resultSet.getString("uuid")), resultSet.getDouble("score")));
                    }
                }
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
            return result;
        }, plugin.database().executor());
    }

    public CompletableFuture<List<Entry>> topJobEarnings(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<Entry> result = new ArrayList<>();
            String sql = """
                    SELECT uuid, SUM(total_earnings) AS score FROM player_jobs
                    GROUP BY uuid ORDER BY score DESC LIMIT ?
                    """;
            try (PreparedStatement statement = plugin.database().connection().prepareStatement(sql)) {
                statement.setInt(1, limit);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        result.add(new Entry(resolveName(resultSet.getString("uuid")), resultSet.getDouble("score")));
                    }
                }
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
            return result;
        }, plugin.database().executor());
    }

    private List<Entry> queryTop(String sql, int limit) {
        List<Entry> result = new ArrayList<>();
        try (PreparedStatement statement = plugin.database().connection().prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(new Entry(resolveName(resultSet.getString("uuid")), resultSet.getDouble("score")));
                }
            }
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
        return result;
    }

    private String resolveName(String uuidString) {
        try {
            java.util.UUID uuid = java.util.UUID.fromString(uuidString);
            String name = plugin.getServer().getOfflinePlayer(uuid).getName();
            return name == null ? uuidString.substring(0, 8) : name;
        } catch (IllegalArgumentException exception) {
            return uuidString;
        }
    }
}