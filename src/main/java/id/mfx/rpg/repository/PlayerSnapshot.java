package id.mfx.rpg.repository;

import id.mfx.rpg.model.PlayerJobData;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record PlayerSnapshot(
        UUID uuid,
        String rankId,
        long rankXp,
        int prestige,
        int dailyStreak,
        int longestDailyStreak,
        LocalDate lastDailyClaim,
        Map<String, PlayerJobData> jobs
) {
    public static PlayerSnapshot fresh(UUID uuid, String starterRank) {
        return new PlayerSnapshot(uuid, starterRank, 0L, 0, 0, 0, null, Map.of());
    }
}