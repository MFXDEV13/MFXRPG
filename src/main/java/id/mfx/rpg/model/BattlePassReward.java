package id.mfx.rpg.model;

import org.bukkit.Material;

import java.util.List;

public record BattlePassReward(
        String displayName,
        Material material,
        List<String> commands
) {}