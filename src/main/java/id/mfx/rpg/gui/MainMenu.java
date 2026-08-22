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
import java.util.Map;

public final class MainMenu extends BaseMenu {

    private static final DecimalFormat BALANCE_FORMAT = new DecimalFormat("#,##0.00");

    public MainMenu(MFXRPG plugin) {
        super(plugin, 54, plugin.text().parse(plugin.theme().getString("main-menu.title", "<light_purple>MFX RPG</light_purple>")));
    }

    @Override
    protected void render(Player player, Inventory inventory) {
        Material filler = Material.matchMaterial(plugin.theme().getString("main-menu.filler-material", "BLACK_STAINED_GLASS_PANE"));
        fill(inventory, filler == null ? Material.BLACK_STAINED_GLASS_PANE : filler, plugin.theme().getString("main-menu.filler-name", "<dark_gray>"));
        double balance = plugin.vaultHook().economy().map(economy -> economy.getBalance(player)).orElse(0.0D);
        ConfigurationSection profile = plugin.theme().getConfigurationSection("profile");
        if (profile != null) {
            set(inventory, 4, new ItemBuilder(plugin, Material.PLAYER_HEAD)
                    .name(profile.getString("name"), Map.of("player", Component.text(player.getName()), "balance", Component.text(BALANCE_FORMAT.format(balance))))
                    .lore(profile.getStringList("lore"), Map.of("player", Component.text(player.getName()), "balance", Component.text(BALANCE_FORMAT.format(balance))))
                    .action("profile").build());
        }
        addButton(inventory, "rank", "rank");
        addButton(inventory, "jobs", "jobs");
        addButton(inventory, "quests", "quests");
        addButton(inventory, "daily", "daily");
        addButton(inventory, "achievements", "achievements");
        addButton(inventory, "top", "top");
        addButton(inventory, "crates", "crates");
        addButton(inventory, "battle-pass", "battle-pass");
        addButton(inventory, "settings", "settings");
        set(inventory, 49, new ItemBuilder(plugin, Material.BARRIER)
                .name(plugin.theme().getString("common.close.name"))
                .lore(plugin.theme().getStringList("common.close.lore"))
                .action("close").build());
    }

    private void addButton(Inventory inventory, String key, String action) {
        ConfigurationSection section = plugin.theme().getConfigurationSection(key);
        if (section == null) return;
        Material material = Material.matchMaterial(section.getString("material", "STONE"));
        set(inventory, section.getInt("slot"), new ItemBuilder(plugin, material == null ? Material.STONE : material)
                .name(section.getString("name"))
                .lore(section.getStringList("lore"))
                .action(action).build());
    }

    @Override
    public void click(Player player, InventoryClickEvent event, String action, String payload) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
        switch (action) {
            case "close" -> player.closeInventory();
            case "crates" -> player.openInventory(new CrateGui(plugin.crateService()).getInventory());
            case "battle-pass" -> player.openInventory(new BattlePassGui(plugin.battlePassService()).getInventory());
            case "rank" -> new RankMenu(plugin).open(player);
            case "jobs" -> new JobsMenu(plugin).open(player);
            case "daily" -> new DailyMenu(plugin).open(player);
            case "quests" -> new QuestsMenu(plugin).open(player);
            case "achievements" -> new AchievementsMenu(plugin).open(player);
            case "top" -> new TopMenu(plugin).open(player);
            case "profile" -> new ProfileMenu(plugin, Bukkit.getOfflinePlayer(player.getUniqueId())).open(player);
            default -> plugin.text().send(player, plugin.message("common.feature-coming-soon"), Map.of("feature", Component.text(action.replace('-', ' ').toUpperCase())));
        }
    }
}