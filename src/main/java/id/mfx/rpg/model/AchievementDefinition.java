package id.mfx.rpg.model;

public record AchievementDefinition(
        String id,
        String category,
        String type,
        String target,
        long amount,
        String name,
        String icon,
        double rewardMoney,
        int points
) {
}