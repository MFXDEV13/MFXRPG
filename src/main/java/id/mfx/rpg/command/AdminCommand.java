package id.mfx.rpg.command;

import id.mfx.rpg.MFXRPG;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class AdminCommand implements CommandExecutor {

    private final MFXRPG plugin;

    public AdminCommand(MFXRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("mfxrpg.admin")) {
            plugin.text().send(sender, plugin.message("common.no-permission"));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadAllConfigurations();
            plugin.text().send(sender, plugin.message("common.reload-success"));
            return true;
        }

        plugin.text().send(sender, "<gray>Usage: <aqua>/mfxrpg reload</aqua>");
        return true;
    }
}