package id.mfx.rpg.hook;

import id.mfx.rpg.MFXRPG;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public final class MfxRpgPlaceholderExpansion extends PlaceholderExpansion {

    private final MFXRPG plugin;

    public MfxRpgPlaceholderExpansion(MFXRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "mfxrpg";
    }

    @Override
    public @NotNull String getAuthor() {
        return "MFX";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("battlepass_season")) {
            return plugin.battlePassService().seasonId();
        }

        if (params.equalsIgnoreCase("battlepass_max_level")) {
            return String.valueOf(plugin.battlePassService().maxLevel());
        }

        if (params.equalsIgnoreCase("battlepass_xp_per_level")) {
            return String.valueOf(plugin.battlePassService().xpPerLevel());
        }

        if (params.startsWith("crate_keys_")) {
            if (player == null || player.getUniqueId() == null) {
                return "0";
            }

            String crateId = params.substring("crate_keys_".length());

            return String.valueOf(
                    plugin.crateService()
                            .getKeys(player.getUniqueId(), crateId)
                            .getNow(0)
            );
        }

        return null;
    }
}