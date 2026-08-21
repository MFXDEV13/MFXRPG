package id.mfx.rpg.command;

import id.mfx.rpg.MFXRPG;
import id.mfx.rpg.gui.ProfileMenu;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ProfileCommand implements CommandExecutor {
    private final MFXRPG plugin;
    public ProfileCommand(MFXRPG plugin) { this.plugin = plugin; }
    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) { plugin.text().send(sender, plugin.message("common.player-only")); return true; }

        OfflinePlayer target = player;
        if (args.length == 1) {
            if (!player.hasPermission("mfxrpg.profile.others")) {
                plugin.text().send(player, plugin.message("common.no-permission"));
                return true;
            }
            target = Bukkit.getOfflinePlayer(args[0]);
        }

        new ProfileMenu(plugin, target).open(player);
        return true;
    }
}