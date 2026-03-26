package github.com.railgun19457.dummy.gui;

import github.com.railgun19457.dummy.api.model.DummySession;
import github.com.railgun19457.dummy.api.model.DummyTraits;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DummyGuiService {

	private static final String TITLE_PREFIX = "Dummy Config: ";
	private static final String QUICK_TITLE_PREFIX = "Dummy Menu: ";
	private static final int SLOT_OPEN_INVENTORY = 11;
	private static final int SLOT_OPEN_EQUIPMENT = 13;
	private static final int SLOT_OPEN_CONFIG = 15;

	private final Map<UUID, String> editingDummyByViewer = new ConcurrentHashMap<>();
	private final Map<UUID, String> quickMenuDummyByViewer = new ConcurrentHashMap<>();

	public void openQuickMenu(Player viewer, DummySession session) {
		Inventory inventory = Bukkit.createInventory(new QuickHolder(), 27, Component.text(QUICK_TITLE_PREFIX + session.name()));
		inventory.setItem(SLOT_OPEN_INVENTORY, actionItem(Material.CHEST, "背包", "打开假人完整背包"));
		inventory.setItem(SLOT_OPEN_EQUIPMENT, actionItem(Material.IRON_CHESTPLATE, "装备栏", "查看假人装备槽"));
		inventory.setItem(SLOT_OPEN_CONFIG, actionItem(Material.COMPARATOR, "假人配置", "打开配置开关菜单"));
		quickMenuDummyByViewer.put(viewer.getUniqueId(), session.name());
		viewer.openInventory(inventory);
	}

	public void openConfigGui(Player viewer, DummySession session) {
		Inventory inventory = Bukkit.createInventory(new ConfigHolder(), 27, Component.text(TITLE_PREFIX + session.name()));
		DummyTraits traits = session.traits() == null ? new DummyTraits(true, false, false, false) : session.traits();
		inventory.setItem(10, toggleItem("碰撞", "collision", traits.collision()));
		inventory.setItem(12, toggleItem("无敌", "invulnerable", traits.invulnerable()));
		inventory.setItem(14, toggleItem("自动补货", "auto-restock", traits.autoRestock()));
		inventory.setItem(16, toggleItem("自动钓鱼", "auto-fishing", traits.autoFishing()));

		editingDummyByViewer.put(viewer.getUniqueId(), session.name());
		viewer.openInventory(inventory);
	}

	public boolean isConfigTitle(String title) {
		return title != null && title.startsWith(TITLE_PREFIX);
	}

	public boolean isConfigInventory(Inventory inventory) {
		return inventory != null && inventory.getHolder() instanceof ConfigHolder;
	}

	public boolean isQuickMenuInventory(Inventory inventory) {
		return inventory != null && inventory.getHolder() instanceof QuickHolder;
	}

	public String getQuickMenuDummyName(UUID viewerId) {
		return quickMenuDummyByViewer.get(viewerId);
	}

	public void clearQuickMenu(UUID viewerId) {
		quickMenuDummyByViewer.remove(viewerId);
	}

	public QuickAction quickActionBySlot(int rawSlot) {
		return switch (rawSlot) {
			case SLOT_OPEN_INVENTORY -> QuickAction.OPEN_INVENTORY;
			case SLOT_OPEN_EQUIPMENT -> QuickAction.OPEN_EQUIPMENT;
			case SLOT_OPEN_CONFIG -> QuickAction.OPEN_CONFIG;
			default -> null;
		};
	}

	public String getEditingDummyName(UUID viewerId) {
		return editingDummyByViewer.get(viewerId);
	}

	public void clearEditing(UUID viewerId) {
		editingDummyByViewer.remove(viewerId);
	}

	public void refreshConfigItem(Inventory inventory, String traitKey, DummyTraits traits) {
		if (inventory == null || traitKey == null || traits == null) {
			return;
		}
		switch (traitKey) {
			case "collision" -> inventory.setItem(10, toggleItem("碰撞", "collision", traits.collision()));
			case "invulnerable" -> inventory.setItem(12, toggleItem("无敌", "invulnerable", traits.invulnerable()));
			case "auto-restock" -> inventory.setItem(14, toggleItem("自动补货", "auto-restock", traits.autoRestock()));
			case "auto-fishing" -> inventory.setItem(16, toggleItem("自动钓鱼", "auto-fishing", traits.autoFishing()));
		}
	}

	public String traitKeyBySlot(int rawSlot) {
		return switch (rawSlot) {
			case 10 -> "collision";
			case 12 -> "invulnerable";
			case 14 -> "auto-restock";
			case 16 -> "auto-fishing";
			default -> null;
		};
	}

	private ItemStack toggleItem(String display, String key, boolean enabled) {
		ItemStack item = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.displayName(Component.text(display + " (" + key + ")"));
			meta.lore(java.util.List.of(
					Component.text(enabled ? "已启用" : "已禁用"),
					Component.text("点击切换")
			));
			item.setItemMeta(meta);
		}
		return item;
	}

	private ItemStack actionItem(Material material, String display, String desc) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.displayName(Component.text(display));
			meta.lore(java.util.List.of(Component.text(desc), Component.text("点击打开")));
			item.setItemMeta(meta);
		}
		return item;
	}

	private static final class ConfigHolder implements InventoryHolder {
		@Override
		public Inventory getInventory() {
			return null;
		}
	}

	private static final class QuickHolder implements InventoryHolder {
		@Override
		public Inventory getInventory() {
			return null;
		}
	}

	public enum QuickAction {
		OPEN_INVENTORY,
		OPEN_EQUIPMENT,
		OPEN_CONFIG
	}
}
