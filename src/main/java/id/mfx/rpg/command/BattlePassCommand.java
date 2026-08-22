package id.mfx.rpg.command;

import id.mfx.rpg.gui.BattlePassGui;
import id.mfx.rpg.service.BattlePassService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class BattlePassCommand implements CommandExecutor {

    private final BattlePassService service;

    public BattlePassCommand(BattlePassService service) { this.service = service; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommand ini hanya untuk pemain.");
            return true;
        }
        player.openInventory(new BattlePassGui(service).getInventory());
        return true;
    }
}