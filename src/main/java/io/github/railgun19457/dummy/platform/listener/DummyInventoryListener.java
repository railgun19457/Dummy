package io.github.railgun19457.dummy.platform.listener;

import io.github.railgun19457.dummy.common.InventoryCodec;
import io.github.railgun19457.dummy.core.model.DummyInventoryState;
import io.github.railgun19457.dummy.core.model.DummyTraitSet;
import io.github.railgun19457.dummy.core.service.DummyLifecycleService;
import io.github.railgun19457.dummy.core.service.DummyQueryService;
import io.github.railgun19457.dummy.platform.gui.DummyInventoryHolder;
import io.github.railgun19457.dummy.platform.gui.DummyInventoryView;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class DummyInventoryListener implements Listener {

    private final DummyQueryService queryService;
    private final DummyLifecycleService lifecycleService;

    public DummyInventoryListener(DummyQueryService queryService, DummyLifecycleService lifecycleService) {
        this.queryService = queryService;
        this.lifecycleService = lifecycleService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof DummyInventoryHolder holder)) {
            return;
        }
        if (holder.mode() == DummyInventoryHolder.Mode.INVENTORY) {
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getBottomInventory())) {
                return;
            }
            if (event.getRawSlot() >= 45) {
                event.setCancelled(true);
            }
            return;
        }
        event.setCancelled(true);
        if (holder.mode() != DummyInventoryHolder.Mode.CONFIG || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        queryService.findById(holder.dummyId()).ifPresent(definition -> {
            DummyTraitSet currentTraits = definition.traitSet();
            DummyTraitSet updatedTraits = switch (event.getRawSlot()) {
                case 10 -> new DummyTraitSet(!currentTraits.collidable(), currentTraits.invulnerable(), currentTraits.autoRestock(), currentTraits.allowInventoryOpen(), currentTraits.allowInteractionConfigure());
                case 11 -> new DummyTraitSet(currentTraits.collidable(), !currentTraits.invulnerable(), currentTraits.autoRestock(), currentTraits.allowInventoryOpen(), currentTraits.allowInteractionConfigure());
                case 12 -> new DummyTraitSet(currentTraits.collidable(), currentTraits.invulnerable(), !currentTraits.autoRestock(), currentTraits.allowInventoryOpen(), currentTraits.allowInteractionConfigure());
                case 13 -> new DummyTraitSet(currentTraits.collidable(), currentTraits.invulnerable(), currentTraits.autoRestock(), !currentTraits.allowInventoryOpen(), currentTraits.allowInteractionConfigure());
                case 14 -> new DummyTraitSet(currentTraits.collidable(), currentTraits.invulnerable(), currentTraits.autoRestock(), currentTraits.allowInventoryOpen(), !currentTraits.allowInteractionConfigure());
                default -> null;
            };
            if (updatedTraits != null) {
                lifecycleService.updateTraits(definition.id(), updatedTraits);
                queryService.findById(definition.id()).ifPresent(updatedDefinition -> DummyInventoryView.openConfig(player, updatedDefinition));
            }
        });
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof DummyInventoryHolder holder)) {
            return;
        }
        if (holder.mode() == DummyInventoryHolder.Mode.INVENTORY) {
            if (event.getRawSlots().stream().anyMatch(slot -> slot >= 45)) {
                event.setCancelled(true);
            }
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof DummyInventoryHolder holder) || holder.mode() != DummyInventoryHolder.Mode.INVENTORY) {
            return;
        }
        queryService.findById(holder.dummyId()).ifPresent(definition -> {
            DummyInventoryState inventoryState = new DummyInventoryState(
                    InventoryCodec.encodeAll(slice(event.getInventory(), 0, 36)),
                    InventoryCodec.encodeAll(slice(event.getInventory(), 36, 40)),
                    InventoryCodec.encode(event.getInventory().getItem(40))
            );
            lifecycleService.updateInventory(holder.dummyId(), inventoryState);
        });
    }

    private org.bukkit.inventory.ItemStack[] slice(org.bukkit.inventory.Inventory inventory, int startInclusive, int endExclusive) {
        org.bukkit.inventory.ItemStack[] itemStacks = new org.bukkit.inventory.ItemStack[endExclusive - startInclusive];
        for (int index = startInclusive; index < endExclusive; index++) {
            itemStacks[index - startInclusive] = inventory.getItem(index);
        }
        return itemStacks;
    }
}
