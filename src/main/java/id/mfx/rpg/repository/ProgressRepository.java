package id.mfx.rpg.repository;

import id.mfx.rpg.MFXRPG;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ProgressRepository {

    private final MFXRPG plugin;

    public ProgressRepository(MFXRPG plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<String> loadRotationKey(UUID uuid, String period) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement statement = plugin.database().connection().prepareStatement("""
                    SELECT rotation_key FROM player_quest_rotation WHERE uuid = ? AND period = ?
                    """)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, period);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? result.getString("rotation_key") : null;
                }
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        }, plugin.database().executor());
    }

    public CompletableFuture<Void> saveRotation(UUID uuid, String period, String rotationKey, String questIdsCsv) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement statement = plugin.database().connection().prepareStatement("""
                    INSERT INTO player_quest_rotation (uuid, period, rotation_key, quest_ids)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT(uuid, period) DO UPDATE SET rotation_key = excluded.rotation_key, quest_ids = excluded.quest_ids
                    """)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, period);
                statement.setString(3, rotationKey);
                statement.setString(4, questIdsCsv);
                statement.executeUpdate();
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        }, plugin.database().executor());
    }

    public CompletableFuture<String> loadQuestIds(UUID uuid, String period) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement statement = plugin.database().connection().prepareStatement("""
                    SELECT quest_ids FROM player_quest_rotation WHERE uuid = ? AND period = ?
                    """)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, period);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? result.getString("quest_ids") : "";
                }
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        }, plugin.database().executor());
    }

    public CompletableFuture<Map<String, long[]>> loadQuestState(UUID uuid, String period) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, long[]> state = new HashMap<>();
            try (PreparedStatement statement = plugin.database().connection().prepareStatement("""
                    SELECT quest_id, progress, claimed FROM player_quest_state WHERE uuid = ? AND period = ?
                    """)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, period);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        state.put(result.getString("quest_id"), new long[]{
                                result.getLong("progress"),
                                result.getLong("claimed")
                        });
                    }
                }
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
            return state;
        }, plugin.database().executor());
    }

    public CompletableFuture<Void> upsertQuestState(UUID uuid, String period, String questId, long progress, boolean claimed, String rotationKey) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement statement = plugin.database().connection().prepareStatement("""
                    INSERT INTO player_quest_state (uuid, period, quest_id, progress, claimed, rotation_key)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT(uuid, period, quest_id) DO UPDATE SET
                        progress = excluded.progress,
                        claimed = excluded.claimed,
                        rotation_key = excluded.rotation_key
                    """)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, period);
                statement.setString(3, questId);
                statement.setLong(4, progress);
                statement.setLong(5, claimed ? 1 : 0);
                statement.setString(6, rotationKey);
                statement.executeUpdate();
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        }, plugin.database().executor());
    }

    public CompletableFuture<Map<String, long[]>> loadAchievements(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, long[]> state = new HashMap<>();
            try (PreparedStatement statement = plugin.database().connection().prepareStatement("""
                    SELECT achievement_id, progress, unlocked FROM player_achievements WHERE uuid = ?
                    """)) {
                statement.setString(1, uuid.toString());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        state.put(result.getString("achievement_id"), new long[]{
                                result.getLong("progress"),
                                result.getLong("unlocked")
                        });
                    }
                }
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
            return state;
        }, plugin.database().executor());
    }

    public CompletableFuture<Void> upsertAchievement(UUID uuid, String achievementId, long progress, boolean unlocked) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement statement = plugin.database().connection().prepareStatement("""
                    INSERT INTO player_achievements (uuid, achievement_id, progress, unlocked, unlocked_at)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT(uuid, achievement_id) DO UPDATE SET
                        progress = excluded.progress,
                        unlocked = excluded.unlocked,
                        unlocked_at = CASE WHEN excluded.unlocked = 1 AND player_achievements.unlocked = 0
                                           THEN excluded.unlocked_at ELSE player_achievements.unlocked_at END
                    """)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, achievementId);
                statement.setLong(3, progress);
                statement.setLong(4, unlocked ? 1 : 0);
                statement.setLong(5, System.currentTimeMillis());
                statement.executeUpdate();
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        }, plugin.database().executor());
    }

    public CompletableFuture<java.util.List<Object[]>> topByCoinsPlaceholder() {
        return CompletableFuture.completedFuture(java.util.List.of());
    }
}