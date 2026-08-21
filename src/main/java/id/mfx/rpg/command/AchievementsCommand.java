package id.mfx.rpg.command;

import id.mfx.rpg.MFXRPG;
import id.mfx.rpg.gui.AchievementsMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class AchievementsCommand implements CommandExecutor {
    private final MFXRPG plugin;
    public AchievementsCommand(MFXRPG plugin) { this.plugin = plugin; }
    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) { plugin.text().send(sender, plugin.message("common.player-only")); return true; }
        new AchievementsMenu(plugin).open(player);
        return true;
    }
}