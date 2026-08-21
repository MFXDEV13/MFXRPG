package id.mfx.rpg.gui;

import id.mfx.rpg.MFXRPG;
import id.mfx.rpg.model.RankDefinition;
import id.mfx.rpg.repository.PlayerSnapshot;
import id.mfx.rpg.util.ItemBuilder;
import id.mfx.rpg.util.ProgressBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Optional;

public final class RankMenu extends BaseMenu {

    public RankMenu(MFXRPG plugin) {
        super(plugin, 54, plugin.text().parse("<gradient:#A855F7:#06B6D4><bold>✦ RANK & PRESTIGE ✦</bold></gradient>"));
    }

    @Override
    protected void render(Player player, Inventory inventory) {
        fill(inventory, Material.BLACK_STAINED_GLASS_PANE, "<dark_gray>");
        plugin.rankService().load(player.getUniqueId()).thenAccept(snapshot -> plugin.getServer().getScheduler().runTask(plugin, () -> renderLoaded(player, inventory, snapshot)));
    }

    private void renderLoaded(Player player, Inventory inventory, PlayerSnapshot snapshot) {
        RankDefinition current = plugin.rankService().rank(snapshot.rankId()).orElseThrow();
        Optional<RankDefinition> next = plugin.rankService().nextRank(snapshot);

        set(inventory, 4, new ItemBuilder(plugin, Material.matchMaterial(current.icon()) == null ? Material.NETHER_STAR : Material.matchMaterial(current.icon()))
                .name("<gradient:#A855F7:#06B6D4><bold>RANK SAAT INI</bold></gradient>")
                .lore(List.of(
                        "<gray>Rank: " + current.displayName(),
                        "<gray>Prestige: <gold>" + snapshot.prestige() + "</gold>",
                        "<gray>Rank XP: <aqua>" + snapshot.rankXp() + "</aqua>"
                ))
                .build());

        if (next.isPresent()) {
            RankDefinition target = next.get();
            set(inventory, 22, new ItemBuilder(plugin, Material.matchMaterial(target.icon()) == null ? Material.NETHER_STAR : Material.matchMaterial(target.icon()))
                    .name("<gradient:#FACC15:#FB923C><bold>RANK BERIKUTNYA</bold></gradient>")
                    .lore(List.of(
                            "<gray>Target: " + target.displayName(),
                            "<gray>XP: <aqua>" + snapshot.rankXp() + "</aqua><dark_gray>/</dark_gray><white>" + target.requiredXp() + "</white>",
                            ProgressBar.render(snapshot.rankXp(), target.requiredXp(), 18),
                            "<gray>Biaya: <gold>" + target.price() + " Coins</gold>",
                            "",
                            "<yellow><bold>▶ Klik untuk rank-up</bold>"
                    ))
                    .action("rankup")
                    .build());
        } else {
            set(inventory, 22, new ItemBuilder(plugin, Material.NETHER_STAR)
                    .name("<gradient:#FACC15:#FB923C><bold>✦ RANK MAKSIMUM ✦</bold></gradient>")
                    .lore(List.of(
                            "<gray>Kamu telah mencapai rank tertinggi.",
                            "<gray>Prestige untuk reset rank dan memperoleh bonus permanen.",
                            "",
                            "<gold><bold>▶ Klik untuk prestige</bold>"
                    ))
                    .action("prestige")
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
        if (action.equals("rankup")) {
            new ConfirmMenu(plugin, "<gold><bold>Konfirmasi Rank-up</bold></gold>", "<gray>Biaya akan dipotong dari saldo Vault/Economy.", this::performRankup).open(player);
            return;
        }
        if (action.equals("prestige")) {
            new ConfirmMenu(plugin, "<light_purple><bold>Konfirmasi Prestige</bold></light_purple>", "<gray>Rank dan Rank XP akan direset ke awal.", this::performPrestige).open(player);
        }
    }

    private void performRankup(Player player) {
        plugin.rankService().rankup(player).thenAccept(result -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!result.success()) {
                plugin.text().send(player, result.message());
                return;
            }
            player.showTitle(net.kyori.adventure.title.Title.title(
                    plugin.text().parse("<gradient:#FACC15:#FB923C><bold>RANK UP!</bold></gradient>"),
                    plugin.text().parse("<gray>Kamu menjadi " + result.rank().displayName() + "</gray>")
            ));
            new RankMenu(plugin).open(player);
        }));
    }

    private void performPrestige(Player player) {
        plugin.rankService().prestige(player).thenAccept(result -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!result.success()) {
                plugin.text().send(player, result.message());
                return;
            }
            player.showTitle(net.kyori.adventure.title.Title.title(
                    plugin.text().parse("<gradient:#C084FC:#FACC15><bold>PRESTIGE " + result.prestige() + "!</bold></gradient>"),
                    plugin.text().parse("<gray>Bonus Job XP permanen telah meningkat.</gray>")
            ));
            new RankMenu(plugin).open(player);
        }));
    }
}