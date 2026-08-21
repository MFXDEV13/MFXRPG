package id.mfx.rpg.gui;

import id.mfx.rpg.MFXRPG;
import id.mfx.rpg.model.JobDefinition;
import id.mfx.rpg.model.PlayerJobData;
import id.mfx.rpg.repository.PlayerSnapshot;
import id.mfx.rpg.util.ItemBuilder;
import id.mfx.rpg.util.ProgressBar;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

public final class JobsMenu extends BaseMenu {

    public JobsMenu(MFXRPG plugin) {
        super(plugin, 54, plugin.text().parse("<gradient:#38BDF8:#6366F1><bold>⚒ JELAJAHI PEKERJAAN</bold></gradient>"));
    }

    @Override
    protected void render(Player player, Inventory inventory) {
        fill(inventory, Material.BLACK_STAINED_GLASS_PANE, "<dark_gray>");
        plugin.playerRepository().loadOrCreate(player.getUniqueId()).thenAccept(snapshot -> plugin.getServer().getScheduler().runTask(plugin, () -> renderLoaded(inventory, snapshot)));
    }

    private void renderLoaded(Inventory inventory, PlayerSnapshot snapshot) {
        int slot = 19;
        for (JobDefinition definition : plugin.jobService().jobs()) {
            PlayerJobData data = snapshot.jobs().get(definition.id());
            List<String> lore = new ArrayList<>(definition.description());
            lore.add("");
            if (data == null) {
                lore.add("<red>Status: <white>Belum bergabung</white>");
                lore.add("<aqua><bold>▶ Klik untuk bergabung</bold>");
            } else {
                long required = plugin.jobService().requiredXp(data.level());
                lore.add("<green>Status: <white>Aktif</white>");
                lore.add("<gray>Level: <gold>" + data.level() + "</gold>");
                lore.add("<gray>XP: <aqua>" + data.xp() + "</aqua><dark_gray>/</dark_gray><white>" + required + "</white>");
                lore.add(ProgressBar.render(data.xp(), required, 16));
                lore.add("<gray>Total pendapatan: <gold>" + String.format("%,.2f", data.totalEarnings()) + "</gold>");
                lore.add("");
                lore.add("<yellow><bold>▶ Klik untuk keluar dari job</bold>");
            }

            Material material = Material.matchMaterial(definition.icon());
            set(inventory, slot++, new ItemBuilder(plugin, material == null ? Material.STONE : material)
                    .name(definition.displayName())
                    .lore(lore)
                    .action(data == null ? "join" : "leave")
                    .payload(definition.id())
                    .build());
        }

        set(inventory, 49, new ItemBuilder(plugin, Material.ARROW)
                .name("<yellow><bold>← KEMBALI</bold></yellow>")
                .lore(List.of("<gray>Kembali ke dashboard."))
                .action("back")
                .build());
    }

    @Override
    public void click(Player player, InventoryClickEvent event, String action, String payload) {
        if (action.equals("back")) {
            new MainMenu(plugin).open(player);
            return;
        }

        if (action.equals("join")) {
            plugin.jobService().join(player, payload).thenAccept(result -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.text().send(player, result.message());
                new JobsMenu(plugin).open(player);
            }));
            return;
        }

        if (action.equals("leave")) {
            new ConfirmMenu(plugin, "<red><bold>Keluar dari Job?</bold></red>", "<gray>Progress Job kamu akan tetap tersimpan.", confirmed ->
                    plugin.jobService().leave(confirmed, payload).thenAccept(result -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                        plugin.text().send(confirmed, result.message());
                        new JobsMenu(plugin).open(confirmed);
                    }))
            ).open(player);
        }
    }
}