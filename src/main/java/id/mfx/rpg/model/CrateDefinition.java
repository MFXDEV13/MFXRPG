package id.mfx.rpg.model;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;

public record CrateDefinition(
        String id,
        String displayName,
        Material icon,
        String keyName,
        int cooldownSeconds,
        boolean pityEnabled,
        List<String> pityTargetRarities,
        int pityGuaranteeAfterMisses,
        Map<String, CrateReward> rewards
) {}