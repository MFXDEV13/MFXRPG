package id.mfx.rpg.model;

public record QuestDefinition(
        String id,
        String period,
        String type,
        String target,
        long amount,
        String name,
        String icon,
        double rewardMoney,
        long rewardJobXp
) {
}