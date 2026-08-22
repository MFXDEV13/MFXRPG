package id.mfx.rpg.model;

public record BattlePassTier(
        int level,
        BattlePassReward freeReward,
        BattlePassReward premiumReward
) {}