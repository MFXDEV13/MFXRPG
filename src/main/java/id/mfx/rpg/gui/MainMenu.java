package id.mfx.rpg.gui;

import id.mfx.rpg.MFXRPG;
import id.mfx.rpg.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

public final class MainMenu extends BaseMenu {

    private static final DecimalFormat BALANCE_FORMAT = new DecimalFormat("#,##0.00");

    public MainMenu(MFXRPG plugin) {
        super(
                plugin,
                54,
                plugin.text().parse(
                        plugin.theme().getString(
                                "main-menu.title",
                                "<light_purple>MFX RPG</light_purple>"
                        )
                )
        );
    }

    @Override
    protected void render(Player player, Inventory inventory) {
        Material filler = Material.matchMaterial(
                plugin.theme().getString(
                        "main-menu.filler-material",
                        "BLACK_STAINED_GLASS_PANE"
                )
        );

        fill(
                inventory,
                filler == null ? Material.BLACK_STAINED_GLASS_PANE : filler,
                plugin.theme().getString("main-menu.filler-name", "<dark_gray>")
        );

        double balance = plugin.vaultHook()
                .economy()
                .map(economy -> economy.getBalance(player))
                .orElse(0.0D);

        renderProfile(player, inventory, balance);

        // Baris kedua: progression dan aktivitas RPG.
        addResourceButton(inventory, "rank", "rank", "rank_prestige", 10);
        addResourceButton(inventory, "jobs", "jobs", "jobs_menu", 11);
        addResourceButton(inventory, "quests", "quests", "quests_menu", 12);
        addResourceButton(inventory, "daily", "daily", "daily_reward", 14);
        addResourceButton(
                inventory,
                "achievements",
                "achievements",
                "reward_claimed",
                15
        );
        addResourceButton(inventory, "top", "top", "hall_of_fame", 16);

        // Baris ketiga: fitur utama Paket 4.
        set(
                inventory,
                29,
                new ItemBuilder(plugin, Material.PAPER)
                        .name("<gradient:gold:yellow>Crates</gradient>")
                        .lore(List.of(
                                "<gray>Buka crate dengan virtual key.</gray>",
                                "",
                                "<yellow>Klik untuk membuka</yellow>"
                        ))
                        .itemModel("mfxrpg", "crate_menu")
                        .action("crates")
                        .build()
        );

        set(
                inventory,
                33,
                new ItemBuilder(plugin, Material.PAPER)
                        .name("<gradient:#dba9ff:#ff77d9>Battle Pass</gradient>")
                        .lore(List.of(
                                "<gray>Lihat progres season dan klaim reward.</gray>",
                                "",
                                "<yellow>Klik untuk membuka</yellow>"
                        ))
                        .itemModel("mfxrpg", "battlepass_menu")
                        .action("battle-pass")
                        .build()
        );

        // Bagian utilitas.
        addResourceButton(
                inventory,
                "settings",
                "settings",
                "settings_menu",
                40
        );

        set(
                inventory,
                49,
                new ItemBuilder(plugin, Material.PAPER)
                        .name("<red>Tutup Menu</red>")
                        .lore(List.of("<gray>Klik untuk menutup menu RPG.</gray>"))
                        .itemModel("mfxrpg", "close_menu")
                        .action("close")
                        .build()
        );
    }

    private void renderProfile(
            Player player,
            Inventory inventory,
            double balance
    ) {
        ConfigurationSection profile = plugin.theme()
                .getConfigurationSection("profile");

        if (profile == null) {
            return;
        }

        set(
                inventory,
                4,
                new ItemBuilder(plugin, Material.PAPER)
                        .name(
                                profile.getString(
                                        "name",
                                        "<gradient:#dba9ff:#ff77d9>%player%</gradient>"
                                ),
                                Map.of(
                                        "player", Component.text(player.getName()),
                                        "balance", Component.text(
                                                BALANCE_FORMAT.format(balance)
                                        )
                                )
                        )
                        .lore(
                                profile.getStringList("lore"),
                                Map.of(
                                        "player", Component.text(player.getName()),
                                        "balance", Component.text(
                                                BALANCE_FORMAT.format(balance)
                                        )
                                )
                        )
                        .itemModel("mfxrpg", "profile_menu")
                        .action("profile")
                        .build()
        );
    }

    private void addResourceButton(
            Inventory inventory,
            String themeKey,
            String action,
            String modelId,
            int slot
    ) {
        ConfigurationSection section = plugin.theme()
                .getConfigurationSection(themeKey);

        if (section == null) {
            return;
        }

        set(
                inventory,
                slot,
                new ItemBuilder(plugin, Material.PAPER)
                        .name(section.getString("name", "<white>" + themeKey + "</white>"))
                        .lore(section.getStringList("lore"))
                        .itemModel("mfxrpg", modelId)
                        .action(action)
                        .build()
        );
    }

    @Override
    public void click(
            Player player,
            InventoryClickEvent event,
            String action,
            String payload
    ) {
        player.playSound(
                player.getLocation(),
                Sound.UI_BUTTON_CLICK,
                0.7F,
                1.2F
        );

        switch (action) {
            case "close" -> player.closeInventory();

            case "crates" -> player.openInventory(
                    new CrateGui(plugin.crateService()).getInventory()
            );

            case "battle-pass" -> player.openInventory(
                    new BattlePassGui(plugin.battlePassService()).getInventory()
            );

            case "rank" -> new RankMenu(plugin).open(player);
            case "jobs" -> new JobsMenu(plugin).open(player);
            case "daily" -> new DailyMenu(plugin).open(player);
            case "quests" -> new QuestsMenu(plugin).open(player);
            case "achievements" -> new AchievementsMenu(plugin).open(player);
            case "top" -> new TopMenu(plugin).open(player);

            case "profile" -> new ProfileMenu(
                    plugin,
                    Bukkit.getOfflinePlayer(player.getUniqueId())
            ).open(player);

            default -> plugin.text().send(
                    player,
                    plugin.message("common.feature-coming-soon"),
                    Map.of(
                            "feature",
                            Component.text(
                                    action.replace('-', ' ').toUpperCase()
                            )
                    )
            );
        }
    }
}