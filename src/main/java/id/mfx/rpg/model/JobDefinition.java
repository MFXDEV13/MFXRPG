package id.mfx.rpg.model;

import java.util.List;
import java.util.Map;

public record JobDefinition(
        String id,
        String displayName,
        String icon,
        List<String> description,
        Map<String, Reward> blockBreakRewards,
        Map<String, Reward> entityKillRewards,
        Map<String, Reward> fishCatchRewards
) {
    public record Reward(double money, long xp) {
    }
}