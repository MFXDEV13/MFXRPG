package id.mfx.rpg.service;

import id.mfx.rpg.MFXRPG;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public final class RewardService {

    private final MFXRPG plugin;

    public RewardService(MFXRPG plugin) {
        this.plugin = plugin;
    }

    public boolean deposit(Player player, double amount) {
        if (amount <= 0.0D) {
            return true;
        }
        return plugin.vaultHook().economy()
                .map(economy -> {
                    EconomyResponse response = economy.depositPlayer(player, amount);
                    return response.transactionSuccess();
                })
                .orElse(false);
    }

    public boolean withdraw(Player player, double amount) {
        if (amount <= 0.0D) {
            return true;
        }
        return plugin.vaultHook().economy()
                .map(economy -> {
                    if (!economy.has(player, amount)) {
                        return false;
                    }
                    EconomyResponse response = economy.withdrawPlayer(player, amount);
                    return response.transactionSuccess();
                })
                .orElse(false);
    }

    public void executeConsoleCommands(Player player, List<String> commands) {
        for (String command : commands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
        }
    }
}