package id.mfx.rpg.command;

import id.mfx.rpg.MFXRPG;
import id.mfx.rpg.gui.MainMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class RpgCommand implements CommandExecutor {

    private final MFXRPG plugin;

    public RpgCommand(MFXRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.text().send(sender, plugin.message("common.player-only"));
            return true;
        }

        if (!player.hasPermission("mfxrpg.use")) {
            plugin.text().send(player, plugin.message("common.no-permission"));
            return true;
        }

        new MainMenu(plugin).open(player);
        return true;
    }
}