package id.mfx.rpg.gui;

import id.mfx.rpg.MFXRPG;
import id.mfx.rpg.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class GuiItemFactory {

    private GuiItemFactory() {
    }

    public static ItemStack model(MFXRPG plugin, String modelId, String name) {
        return new ItemBuilder(plugin, Material.PAPER)
                .name(name)
                .itemModel("mfxrpg", modelId)
                .build();
    }
}