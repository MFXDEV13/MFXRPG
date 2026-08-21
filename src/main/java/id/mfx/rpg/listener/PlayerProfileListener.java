package id.mfx.rpg.listener;

import id.mfx.rpg.MFXRPG;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerProfileListener implements Listener {

    private final MFXRPG plugin;

    public PlayerProfileListener(MFXRPG plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.playerRepository().loadOrCreate(event.getPlayer().getUniqueId())
                .exceptionally(throwable -> {
                    plugin.getLogger().warning("Gagal memuat data " + event.getPlayer().getName() + ": " + throwable.getMessage());
                    return null;
                });
    }
}