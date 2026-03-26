package github.com.railgun19457.dummy.listener;

import github.com.railgun19457.dummy.DummyPlugin;
import github.com.railgun19457.dummy.core.manager.DummyManager;
import github.com.railgun19457.dummy.api.model.DummySession;
import github.com.railgun19457.dummy.gui.DummyGuiService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

public final class DummyInteractListener implements Listener {
	private final DummyPlugin plugin;
	private final DummyManager dummyManager;
	private final DummyGuiService guiService;

	public DummyInteractListener(DummyPlugin plugin, DummyManager dummyManager, DummyGuiService guiService) {
		this.plugin = plugin;
		this.dummyManager = dummyManager;
		this.guiService = guiService;
	}

	@EventHandler
	public void onPlayerInteractDummy(PlayerInteractEntityEvent event) {
		if (!(event.getRightClicked() instanceof Player target)) {
			return;
		}

		Player viewer = event.getPlayer();
		DummySession session = dummyManager.findByName(target.getName()).orElse(null);
		if (session == null) {
			return;
		}

		if (!dummyManager.canManage(viewer, session)) {
			return;
		}

		guiService.openQuickMenu(viewer, session);
		event.setCancelled(true);
	}

	@EventHandler
	public void onQuickMenuClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player viewer)) {
			return;
		}
		if (!guiService.isQuickMenuInventory(event.getView().getTopInventory())) {
			return;
		}

		event.setCancelled(true);
		if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
			return;
		}

		DummyGuiService.QuickAction action = guiService.quickActionBySlot(event.getRawSlot());
		if (action == null) {
			return;
		}

		String dummyName = guiService.getQuickMenuDummyName(viewer.getUniqueId());
		if (dummyName == null) {
			return;
		}

		DummySession session = dummyManager.findByName(dummyName).orElse(null);
		if (session == null || !dummyManager.canManage(viewer, session)) {
			viewer.closeInventory();
			return;
		}

		Player target = Bukkit.getPlayerExact(session.name());
		if (target == null || !target.isOnline()) {
			viewer.sendMessage("§c[Dummy] 目标假人当前不在线。");
			viewer.closeInventory();
			return;
		}

		switch (action) {
			case OPEN_INVENTORY, OPEN_EQUIPMENT -> {
				viewer.openInventory(target.getInventory());
				viewer.sendMessage("§a[Dummy] 已打开假人背包: §f" + target.getName());
			}
			case OPEN_CONFIG -> guiService.openConfigGui(viewer, session);
		}
	}

	@EventHandler
	public void onConfigInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}
		if (!guiService.isConfigInventory(event.getView().getTopInventory())) {
			return;
		}

		event.setCancelled(true);
		if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
			return;
		}

		String traitKey = guiService.traitKeyBySlot(event.getRawSlot());
		if (traitKey == null) {
			return;
		}

		String dummyName = guiService.getEditingDummyName(player.getUniqueId());
		if (dummyName == null) {
			return;
		}

		DummyManager.OperationResult result = dummyManager.toggleTrait(player, dummyName, traitKey);
		if (!result.success()) {
			player.sendMessage("§c[Dummy] " + result.message());
			return;
		}

		DummySession updated = result.session();
		if (updated != null) {
			guiService.refreshConfigItem(event.getView().getTopInventory(), traitKey, updated.traits());
			player.updateInventory();
		}
	}

	@EventHandler
	public void onDummyDamaged(EntityDamageByEntityEvent event) {
		if (!(event.getEntity() instanceof Player target)) {
			return;
		}

		DummySession session = dummyManager.findByName(target.getName()).orElse(null);
		if (session == null) {
			return;
		}
		if (session.traits() != null && session.traits().invulnerable()) {
			return;
		}

		Entity damager = event.getDamager();
		if (damager instanceof org.bukkit.entity.Projectile projectile) {
			ProjectileSource source = projectile.getShooter();
			if (source instanceof Entity sourceEntity) {
				damager = sourceEntity;
			}
		}

		if (!(damager instanceof LivingEntity attacker)) {
			return;
		}

		Vector knockback = target.getLocation().toVector().subtract(attacker.getLocation().toVector());
		if (knockback.lengthSquared() < 0.0001D) {
			return;
		}
		knockback.normalize().multiply(0.42D).setY(0.35D);
		Bukkit.getScheduler().runTask(plugin, () -> target.setVelocity(knockback));
	}

	@EventHandler
	public void onDummyDeath(PlayerDeathEvent event) {
		Player target = event.getEntity();
		DummySession session = dummyManager.findByName(target.getName()).orElse(null);
		if (session == null) {
			return;
		}
		if (!dummyManager.config().removeOnDeath()) {
			return;
		}

		Bukkit.getScheduler().runTask(plugin, () -> {
			if (dummyManager.config().instantRespawnOnDeath()) {
				try {
					target.spigot().respawn();
				} catch (Throwable ignored) {
				}
			}
			dummyManager.removeSystem(session.name());
		});
	}

	@EventHandler
	public void onConfigInventoryClose(InventoryCloseEvent event) {
		if (guiService.isQuickMenuInventory(event.getView().getTopInventory())) {
			guiService.clearQuickMenu(event.getPlayer().getUniqueId());
		}
		if (!guiService.isConfigInventory(event.getView().getTopInventory())) {
			return;
		}
		guiService.clearEditing(event.getPlayer().getUniqueId());
	}
}
