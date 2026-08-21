package id.mfx.rpg.hook;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public final class VaultHook {

    private final JavaPlugin plugin;
    private Economy economy;

    public VaultHook(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> registration = plugin.getServer()
                .getServicesManager()
                .getRegistration(Economy.class);

        if (registration == null || registration.getProvider() == null) {
            return false;
        }

        economy = registration.getProvider();
        return true;
    }

    public Optional<Economy> economy() {
        return Optional.ofNullable(economy);
    }

    public boolean available() {
        return economy != null;
    }
}