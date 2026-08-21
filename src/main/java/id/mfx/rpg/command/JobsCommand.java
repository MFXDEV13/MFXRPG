package id.mfx.rpg.command;

import id.mfx.rpg.MFXRPG;
import id.mfx.rpg.gui.JobsMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class JobsCommand implements CommandExecutor {
    private final MFXRPG plugin;
    public JobsCommand(MFXRPG plugin) { this.plugin = plugin; }
    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) { plugin.text().send(sender, plugin.message("common.player-only")); return true; }
        if (args.length == 0) { new JobsMenu(plugin).open(player); return true; }
        if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            plugin.jobService().join(player, args[1].toLowerCase()).thenAccept(result -> plugin.getServer().getScheduler().runTask(plugin, () -> plugin.text().send(player, result.message())));
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("leave")) {
            plugin.jobService().leave(player, args[1].toLowerCase()).thenAccept(result -> plugin.getServer().getScheduler().runTask(plugin, () -> plugin.text().send(player, result.message())));
            return true;
        }
        plugin.text().send(player, "<gray>Usage: <aqua>/jobs</aqua>, <aqua>/jobs join <job></aqua>, <aqua>/jobs leave <job></aqua>");
        return true;
    }
}