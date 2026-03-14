package io.github.railgun19457.dummy.platform.gui;

import io.github.railgun19457.dummy.core.model.DummyId;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class DummyInventoryHolder implements InventoryHolder {

    public enum Mode {
        VIEW,
        CONFIG,
        INVENTORY
    }

    private final DummyId dummyId;
    private final Mode mode;
    private Inventory inventory;

    public DummyInventoryHolder(DummyId dummyId, Mode mode) {
        this.dummyId = dummyId;
        this.mode = mode;
    }

    public DummyId dummyId() {
        return dummyId;
    }

    public Mode mode() {
        return mode;
    }

    public void attach(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
