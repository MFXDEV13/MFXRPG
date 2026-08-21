package id.mfx.rpg.command;

import id.mfx.rpg.MFXRPG;
import id.mfx.rpg.gui.ConfirmMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class RankupCommand implements CommandExecutor {
    private final MFXRPG plugin;
    public RankupCommand(MFXRPG plugin) { this.plugin = plugin; }
    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) { plugin.text().send(sender, plugin.message("common.player-only")); return true; }
        new ConfirmMenu(plugin, "<gold><bold>Konfirmasi Rank-up</bold></gold>", "<gray>Saldo Vault akan dipotong jika syarat terpenuhi.", target ->
                plugin.rankService().rankup(target).thenAccept(result -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!result.success()) plugin.text().send(target, result.message());
                    else target.sendMessage(plugin.text().parse("<green>Rank kamu sekarang " + result.rank().displayName() + "</green>"));
                }))
        ).open(player);
        return true;
    }
}