package id.mfx.rpg.command;

import id.mfx.rpg.gui.CrateGui;
import id.mfx.rpg.service.CrateService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class CrateCommand implements CommandExecutor, TabCompleter {

    private final CrateService service;

    public CrateCommand(CrateService service) { this.service = service; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 && sender instanceof Player player) {
            player.openInventory(new CrateGui(service).getInventory());
            return true;
        }
        if (args.length == 1 && sender instanceof Player player) {
            service.open(player, args[0]).thenAccept(result -> {
                if (result.success()) player.sendMessage("§aKamu mendapat " + result.reward().displayName());
                else player.sendMessage("§c" + result.messageKey());
            });
            return true;
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("give") && sender.hasPermission("mfxrpg.admin.crate")) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) { sender.sendMessage("§cPlayer tidak online."); return true; }
            try {
                int amount = Integer.parseInt(args[3]);
                service.giveKeys(target.getUniqueId(), args[2], amount).thenRun(() -> sender.sendMessage("§aKey diberikan."));
            } catch (NumberFormatException exception) { sender.sendMessage("§cJumlah harus angka."); }
            return true;
        }
        sender.sendMessage("§e/crate [crateId] | /crate give <player> <crateId> <amount>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return service.crates().stream().map(crate -> crate.id()).toList();
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) return null;
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) return service.crates().stream().map(crate -> crate.id()).toList();
        return List.of();
    }
}