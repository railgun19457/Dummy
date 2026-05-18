package dev.dummy.gui;

import dev.dummy.DummyPlugin;
import dev.dummy.dummy.DummyInstance;
import dev.dummy.dummy.DummyManager;
import dev.dummy.dummy.DummySettings;
import dev.dummy.i18n.I18n;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

public final class DummyGuiListener implements Listener {
    private final DummyPlugin plugin;
    private final DummyManager dummyManager;
    private final I18n i18n;

    public DummyGuiListener(DummyPlugin plugin, DummyManager dummyManager, I18n i18n) {
        this.plugin = plugin;
        this.dummyManager = dummyManager;
        this.i18n = i18n;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player target) || !dummyManager.isDummy(target)) {
            return;
        }
        event.setCancelled(true);
        DummyInstance dummy = dummyManager.get(target.getUniqueId());
        if (dummy != null) {
            openConfigMenu(event.getPlayer(), dummy);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof DummyInventoryHolder) {
            if (event.isShiftClick()) {
                handleDummyInventoryShiftClick(event);
                return;
            }
            if (event.getRawSlot() < event.getInventory().getSize() && !isEditableDummyInventorySlot(event.getRawSlot())) {
                event.setCancelled(true);
            }
            return;
        }

        if (!(event.getInventory().getHolder() instanceof DummyMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        DummyInstance dummy = dummyManager.get(holder.dummyName());
        if (dummy == null) {
            player.closeInventory();
            return;
        }

        switch (event.getRawSlot()) {
            case 10 -> Bukkit.getScheduler().runTask(plugin, () -> openDummyInventory(player, dummy));
            case 12 -> toggle(player, dummy, "invulnerable", dummy.settings().invulnerable());
            case 13 -> toggle(player, dummy, "collision", dummy.settings().collision());
            case 14 -> toggle(player, dummy, "chunk-loader", dummy.settings().chunkLoader());
            case 15 -> toggle(player, dummy, "show-in-tab", dummy.settings().showInTab());
            case 16 -> toggle(player, dummy, "ghost", dummy.settings().ghost());
            default -> {
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof DummyInventoryHolder)) {
            return;
        }
        int topSize = event.getInventory().getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topSize && !isEditableDummyInventorySlot(slot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof DummyInventoryHolder holder)) {
            return;
        }
        DummyInstance dummy = dummyManager.get(holder.dummyName());
        if (dummy == null) {
            return;
        }
        syncDummyInventory(event.getInventory(), dummy);
        dummyManager.save();
    }

    public void openDummyInventory(Player viewer, DummyInstance dummy) {
        DummyInventoryHolder holder = new DummyInventoryHolder(dummy.name());
        Inventory inventory = Bukkit.createInventory(holder, 54, i18n.tr("gui.dummy-inventory-title", dummy.name()));
        holder.inventory(inventory);

        PlayerInventory playerInventory = dummy.player().getInventory();
        ItemStack[] storage = playerInventory.getStorageContents();
        for (int slot = 0; slot < storage.length && slot < 36; slot++) {
            inventory.setItem(slot, storage[slot]);
        }

        for (int slot = 36; slot <= 44; slot++) {
            inventory.setItem(slot, item(Material.BLACK_STAINED_GLASS_PANE, i18n.tr("gui.separator"), ""));
        }
        inventory.setItem(36, item(Material.BLUE_STAINED_GLASS_PANE, i18n.tr("gui.helmet-slot"), ""));
        inventory.setItem(37, item(Material.BLUE_STAINED_GLASS_PANE, i18n.tr("gui.chestplate-slot"), ""));
        inventory.setItem(38, item(Material.BLUE_STAINED_GLASS_PANE, i18n.tr("gui.leggings-slot"), ""));
        inventory.setItem(39, item(Material.BLUE_STAINED_GLASS_PANE, i18n.tr("gui.boots-slot"), ""));
        inventory.setItem(41, item(Material.PURPLE_STAINED_GLASS_PANE, i18n.tr("gui.offhand-slot"), ""));
        inventory.setItem(49, item(Material.BLACK_STAINED_GLASS_PANE, i18n.tr("gui.separator"), ""));
        inventory.setItem(51, item(Material.BLACK_STAINED_GLASS_PANE, i18n.tr("gui.separator"), ""));
        inventory.setItem(52, item(Material.BLACK_STAINED_GLASS_PANE, i18n.tr("gui.separator"), ""));
        inventory.setItem(53, item(Material.BLACK_STAINED_GLASS_PANE, i18n.tr("gui.separator"), ""));

        ItemStack[] armor = playerInventory.getArmorContents();
        inventory.setItem(45, armor.length > 3 ? armor[3] : null);
        inventory.setItem(46, armor.length > 2 ? armor[2] : null);
        inventory.setItem(47, armor.length > 1 ? armor[1] : null);
        inventory.setItem(48, armor.length > 0 ? armor[0] : null);
        inventory.setItem(50, playerInventory.getItemInOffHand());
        viewer.openInventory(inventory);
    }

    private void toggle(Player viewer, DummyInstance dummy, String key, boolean currentValue) {
        dummyManager.updateSettings(dummy.name(), key, Boolean.toString(!currentValue));
        openConfigMenu(viewer, dummyManager.require(dummy.name()));
    }

    public void openConfigMenu(Player viewer, DummyInstance dummy) {
        DummyMenuHolder holder = new DummyMenuHolder(dummy.name());
        Inventory inventory = Bukkit.createInventory(holder, 27, i18n.tr("gui.title", dummy.name()));
        holder.inventory(inventory);
        DummySettings settings = dummy.settings();
        inventory.setItem(10, item(Material.CHEST, i18n.tr("gui.inventory"), i18n.tr("gui.inventory-lore")));
        inventory.setItem(12, toggleItem("gui.invulnerable", settings.invulnerable()));
        inventory.setItem(13, toggleItem("gui.collision", settings.collision()));
        inventory.setItem(14, toggleItem("gui.chunk-loader", settings.chunkLoader()));
        inventory.setItem(15, toggleItem("gui.show-in-tab", settings.showInTab()));
        inventory.setItem(16, toggleItem("gui.ghost", settings.ghost()));
        viewer.openInventory(inventory);
    }

    private ItemStack toggleItem(String nameKey, boolean enabled) {
        return item(
                enabled ? Material.LIME_DYE : Material.GRAY_DYE,
                i18n.tr("gui.toggle", i18n.tr(nameKey), enabled),
                i18n.tr("gui.toggle-lore")
        );
    }

    private ItemStack item(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        if (!lore.isBlank()) {
            meta.lore(java.util.List.of(Component.text(lore)));
        }
        item.setItemMeta(meta);
        return item;
    }

    private boolean isEditableDummyInventorySlot(int rawSlot) {
        return (rawSlot >= 0 && rawSlot < 36) || (rawSlot >= 45 && rawSlot <= 48) || rawSlot == 50;
    }

    private void handleDummyInventoryShiftClick(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack current = event.getCurrentItem();
        if (isEmpty(current)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        int rawSlot = event.getRawSlot();
        if (rawSlot >= 0 && rawSlot < top.getSize()) {
            if (!isEditableDummyInventorySlot(rawSlot)) {
                return;
            }
            top.setItem(rawSlot, moveItemToSlots(event.getView().getBottomInventory(), current, 0, 36));
            return;
        }

        Inventory clicked = event.getClickedInventory();
        if (clicked == null) {
            return;
        }
        clicked.setItem(event.getSlot(), moveItemToSlots(top, current, 0, 36));
    }

    private ItemStack moveItemToSlots(Inventory inventory, ItemStack item, int startInclusive, int endExclusive) {
        ItemStack remaining = item.clone();
        int end = Math.min(endExclusive, inventory.getSize());
        for (int slot = startInclusive; slot < end; slot++) {
            ItemStack target = inventory.getItem(slot);
            if (isEmpty(target) || !target.isSimilar(remaining)) {
                continue;
            }
            int maxStackSize = Math.min(target.getMaxStackSize(), inventory.getMaxStackSize());
            int moved = Math.min(maxStackSize - target.getAmount(), remaining.getAmount());
            if (moved <= 0) {
                continue;
            }
            target.setAmount(target.getAmount() + moved);
            remaining.setAmount(remaining.getAmount() - moved);
            if (remaining.getAmount() <= 0) {
                return null;
            }
        }

        for (int slot = startInclusive; slot < end; slot++) {
            if (!isEmpty(inventory.getItem(slot))) {
                continue;
            }
            int moved = Math.min(Math.min(remaining.getMaxStackSize(), inventory.getMaxStackSize()), remaining.getAmount());
            ItemStack placed = remaining.clone();
            placed.setAmount(moved);
            inventory.setItem(slot, placed);
            remaining.setAmount(remaining.getAmount() - moved);
            if (remaining.getAmount() <= 0) {
                return null;
            }
        }

        return remaining;
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir() || item.getAmount() <= 0;
    }

    private void syncDummyInventory(Inventory inventory, DummyInstance dummy) {
        PlayerInventory playerInventory = dummy.player().getInventory();
        ItemStack[] storage = new ItemStack[playerInventory.getStorageContents().length];
        for (int slot = 0; slot < storage.length && slot < 36; slot++) {
            storage[slot] = inventory.getItem(slot);
        }
        playerInventory.setStorageContents(storage);
        playerInventory.setHelmet(inventory.getItem(45));
        playerInventory.setChestplate(inventory.getItem(46));
        playerInventory.setLeggings(inventory.getItem(47));
        playerInventory.setBoots(inventory.getItem(48));
        playerInventory.setItemInOffHand(inventory.getItem(50));
    }
}
