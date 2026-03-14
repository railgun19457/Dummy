package io.github.railgun19457.dummy.platform.gui;

import io.github.railgun19457.dummy.common.InventoryCodec;
import io.github.railgun19457.dummy.core.model.DummyDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class DummyInventoryView {

    private DummyInventoryView() {
    }

    public static void open(Player player, DummyDefinition definition) {
        DummyInventoryHolder holder = new DummyInventoryHolder(definition.id(), DummyInventoryHolder.Mode.INVENTORY);
        Inventory inventory = Bukkit.createInventory(holder, 54, "Dummy Inv: " + definition.name());
        holder.attach(inventory);

        applyInventoryState(inventory, definition);
        inventory.setItem(45, createInfoItem(Material.NAME_TAG, "Name", List.of(definition.name())));
        inventory.setItem(46, createInfoItem(Material.PLAYER_HEAD, "Owner", List.of(definition.ownerName())));
        inventory.setItem(47, createInfoItem(Material.COMPASS, "World", List.of(definition.lastKnownPosition().worldName())));
        inventory.setItem(48, createInfoItem(Material.FEATHER, "Traits", List.of(
            "collidable=" + definition.traitSet().collidable(),
            "invulnerable=" + definition.traitSet().invulnerable(),
            "autoRestock=" + definition.traitSet().autoRestock()
        )));
        inventory.setItem(49, createInfoItem(Material.COMPARATOR, "Hint", List.of("Sneak-right-click to config", "Close to save inventory")));

        player.openInventory(inventory);
    }

    public static void openConfig(Player player, DummyDefinition definition) {
        DummyInventoryHolder holder = new DummyInventoryHolder(definition.id(), DummyInventoryHolder.Mode.CONFIG);
        Inventory inventory = Bukkit.createInventory(holder, 27, "Config: " + definition.name());
        holder.attach(inventory);

        inventory.setItem(10, createToggleItem(Material.SLIME_BALL, "Collidable", definition.traitSet().collidable()));
        inventory.setItem(11, createToggleItem(Material.SHIELD, "Invulnerable", definition.traitSet().invulnerable()));
        inventory.setItem(12, createToggleItem(Material.HOPPER, "Auto Restock", definition.traitSet().autoRestock()));
        inventory.setItem(13, createToggleItem(Material.CHEST, "Allow Inventory Open", definition.traitSet().allowInventoryOpen()));
        inventory.setItem(14, createToggleItem(Material.COMPARATOR, "Allow Interaction Configure", definition.traitSet().allowInteractionConfigure()));

        player.openInventory(inventory);
    }

    public static void applyInventoryState(Inventory inventory, DummyDefinition definition) {
        org.bukkit.inventory.ItemStack[] contents = InventoryCodec.decodeAll(fill(definition.inventoryState().contents(), 36));
        org.bukkit.inventory.ItemStack[] armor = InventoryCodec.decodeAll(fill(definition.inventoryState().armorContents(), 4));
        org.bukkit.inventory.ItemStack offhand = InventoryCodec.decode(definition.inventoryState().offhandItem());
        for (int slot = 0; slot < contents.length; slot++) {
            inventory.setItem(slot, contents[slot]);
        }
        for (int slot = 0; slot < armor.length; slot++) {
            inventory.setItem(36 + slot, armor[slot]);
        }
        inventory.setItem(40, offhand);
    }

    private static List<String> fill(List<String> source, int size) {
        List<String> filled = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            filled.add(index < source.size() ? source.get(index) : null);
        }
        return filled;
    }

    private static ItemStack createInfoItem(Material material, String title, List<String> lines) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            itemMeta.setDisplayName(title);
            itemMeta.setLore(lines);
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    private static ItemStack createToggleItem(Material material, String title, boolean enabled) {
        return createInfoItem(material, title, List.of("state=" + enabled, "click to toggle"));
    }
}
