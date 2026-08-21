package id.mfx.rpg.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class MenuHolder implements InventoryHolder {

    private final BaseMenu menu;

    public MenuHolder(BaseMenu menu) {
        this.menu = menu;
    }

    public BaseMenu menu() {
        return menu;
    }

    @Override
    public @NotNull Inventory getInventory() {
        throw new UnsupportedOperationException("MenuHolder does not own an inventory instance.");
    }
}