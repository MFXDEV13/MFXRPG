package id.mfx.rpg.model;

import org.bukkit.Material;

import java.util.List;

public record CrateReward(
        String id,
        String displayName,
        String rarity,
        int weight,
        Material material,
        List<String> commands
) {}