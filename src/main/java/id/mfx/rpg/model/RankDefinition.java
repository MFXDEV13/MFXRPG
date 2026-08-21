package id.mfx.rpg.model;

import java.util.List;

public record RankDefinition(
        String id,
        int order,
        String displayName,
        String icon,
        long requiredXp,
        double price,
        List<String> benefits
) {
}