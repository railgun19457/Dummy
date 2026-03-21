package github.com.railgun19457.dummy.core.manager;

import github.com.railgun19457.dummy.core.config.PluginConfig;
import github.com.railgun19457.dummy.core.model.DummyPosition;
import github.com.railgun19457.dummy.core.model.DummySession;
import github.com.railgun19457.dummy.core.model.DummyTraits;
import github.com.railgun19457.dummy.core.storage.InventoryCodec;
import github.com.railgun19457.dummy.nms.NmsBridge;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public final class DummyManager {

	private static final Pattern VALID_NAME = Pattern.compile("^[A-Za-z0-9_]{1,16}$");

	private PluginConfig config;
	private final DummyRegistry registry;
	private final NmsBridge nmsBridge;

	public DummyManager(PluginConfig config, DummyRegistry registry, NmsBridge nmsBridge) {
		this.config = config;
		this.registry = registry;
		this.nmsBridge = nmsBridge;
	}

	public OperationResult spawn(Player requester, String requestedName) {
		String name = resolveName(requester, requestedName);
		if (name.isBlank()) {
			return OperationResult.failed("假人名称不能为空。");
		}

		if (!VALID_NAME.matcher(name).matches()) {
			return OperationResult.failed("假人名称仅支持字母/数字/下划线，且长度 1~16。");
		}

		if (registry.exists(name)) {
			return OperationResult.failed("假人名称已存在: " + name);
		}

		if (registry.size() >= config.globalLimit()) {
			return OperationResult.failed("已达到服务器假人上限(" + config.globalLimit() + ")。");
		}

		boolean admin = requester.hasPermission("dummy.admin");
		List<DummySession> ownerList = registry.listByOwner(requester.getUniqueId());
		if (!admin && ownerList.size() >= config.perPlayerLimit()) {
			return OperationResult.failed("你已达到个人假人上限(" + config.perPlayerLimit() + ")。");
		}

		DummySession session = new DummySession(
				UUID.randomUUID(),
				name,
				requester.getUniqueId(),
				requester.getName(),
				requester.getName(),
				new DummyTraits(
						config.defaultCollision(),
						config.defaultInvulnerable(),
						config.defaultAutoRestock(),
						config.defaultAutoFishing()
				),
				DummyPosition.from(requester.getLocation()),
				InventoryCodec.encode(requester.getInventory().getContents()),
				Instant.now()
		);

		registry.put(session);
		String warning = nmsBridge.spawnDummy(session);
		if (warning != null && warning.startsWith("NMS 生成失败")) {
			registry.remove(name);
			return OperationResult.failed(warning);
		}
		return OperationResult.success(session, "创建成功", warning);
	}

	public PluginConfig config() {
		return config;
	}

	public void reloadConfig(PluginConfig pluginConfig) {
		this.config = pluginConfig;
	}

	public OperationResult updateSkin(Player requester, String name, String skinSource) {
		DummySession session = registry.find(name).orElse(null);
		if (session == null) {
			return OperationResult.failed("未找到假人: " + name);
		}

		boolean admin = requester.hasPermission("dummy.admin");
		if (!admin && !session.ownerUuid().equals(requester.getUniqueId())) {
			return OperationResult.failed("你只能设置自己的假人皮肤。");
		}

		if (skinSource == null || skinSource.isBlank()) {
			return OperationResult.failed("皮肤来源玩家名不能为空。");
		}

		DummySession updated = new DummySession(
				session.id(),
				session.name(),
				session.ownerUuid(),
				session.ownerName(),
				skinSource,
				session.traits(),
				session.position(),
				session.inventoryData(),
				session.createdAt()
		);
		registry.put(updated);
		return OperationResult.success(updated, "皮肤已更新", null);
	}

	public OperationResult toggleTrait(Player requester, String name, String traitKey) {
		DummySession session = registry.find(name).orElse(null);
		if (session == null) {
			return OperationResult.failed("未找到假人: " + name);
		}
		if (!canManage(requester, session)) {
			return OperationResult.failed("你只能配置自己的假人。") ;
		}

		DummyTraits current = session.traits();
		if (current == null) {
			current = new DummyTraits(true, false, false, false);
		}
		DummyTraits updatedTraits = switch (traitKey.toLowerCase(Locale.ROOT)) {
			case "collision" -> current.toggleCollision();
			case "invulnerable" -> current.toggleInvulnerable();
			case "auto-restock" -> current.toggleAutoRestock();
			case "auto-fishing" -> current.toggleAutoFishing();
			default -> null;
		};
		if (updatedTraits == null) {
			return OperationResult.failed("未知配置项: " + traitKey);
		}

		DummySession updated = new DummySession(
				session.id(),
				session.name(),
				session.ownerUuid(),
				session.ownerName(),
				session.skinSource(),
				updatedTraits,
				session.position(),
				session.inventoryData(),
				session.createdAt()
		);
		registry.put(updated);

		Player online = Bukkit.getPlayerExact(session.name());
		if (online != null && online.isOnline()) {
			online.setCollidable(updatedTraits.collision());
			online.setInvulnerable(updatedTraits.invulnerable());
		}

		return OperationResult.success(updated, "配置已更新", null);
	}

	public OperationResult remove(Player requester, String name) {
		if (name == null || name.isBlank()) {
			return OperationResult.failed("名称不能为空。");
		}

		DummySession session = registry.find(name).orElse(null);
		if (session == null) {
			return OperationResult.failed("未找到假人: " + name);
		}

		boolean admin = requester.hasPermission("dummy.admin");
		if (!admin && !session.ownerUuid().equals(requester.getUniqueId())) {
			return OperationResult.failed("你只能移除自己的假人。");
		}

		registry.remove(name);
		nmsBridge.removeDummy(session);
		return OperationResult.success(session, "移除成功", null);
	}

	public OperationResult removeSystem(String name) {
		if (name == null || name.isBlank()) {
			return OperationResult.failed("名称不能为空。");
		}

		DummySession session = registry.find(name).orElse(null);
		if (session == null) {
			return OperationResult.failed("未找到假人: " + name);
		}

		registry.remove(name);
		nmsBridge.removeDummy(session);
		return OperationResult.success(session, "移除成功", null);
	}

	public List<DummySession> listFor(Player requester) {
		if (requester.hasPermission("dummy.admin")) {
			return registry.listAll();
		}
		return registry.listByOwner(requester.getUniqueId());
	}

	public Optional<DummySession> findByName(String name) {
		return registry.find(name);
	}

	public List<DummySession> listAll() {
		return registry.listAll();
	}

	public void restoreSessions(List<DummySession> sessions, boolean respawn) {
		for (DummySession session : sessions) {
			if (session == null || session.name() == null || session.name().isBlank()) {
				continue;
			}
			registry.put(session);
			if (respawn) {
				nmsBridge.spawnDummy(session);
			}
		}
	}

	public void syncSessionPosition(String name, Location location) {
		DummySession session = registry.find(name).orElse(null);
		if (session == null || location == null) {
			return;
		}
		DummySession updated = new DummySession(
				session.id(),
				session.name(),
				session.ownerUuid(),
				session.ownerName(),
				session.skinSource(),
				session.traits(),
				DummyPosition.from(location),
				session.inventoryData(),
				session.createdAt()
		);
		registry.put(updated);
	}

	public void syncSessionInventory(String name) {
		DummySession session = registry.find(name).orElse(null);
		if (session == null) {
			return;
		}
		Player online = Bukkit.getPlayerExact(name);
		if (online == null || !online.isOnline()) {
			return;
		}
		DummySession updated = new DummySession(
				session.id(),
				session.name(),
				session.ownerUuid(),
				session.ownerName(),
				session.skinSource(),
				session.traits(),
				session.position(),
				InventoryCodec.encode(online.getInventory().getContents()),
				session.createdAt()
		);
		registry.put(updated);
	}

	public void applyStoredInventoryIfOnline(String name) {
		DummySession session = registry.find(name).orElse(null);
		if (session == null) {
			return;
		}
		Player online = Bukkit.getPlayerExact(name);
		if (online == null || !online.isOnline()) {
			return;
		}
		try {
			online.getInventory().setContents(InventoryCodec.decode(session.inventoryData()));
		} catch (Exception ignored) {
		}
	}

	public void syncAllOnlineState() {
		for (DummySession session : registry.listAll()) {
			Player online = Bukkit.getPlayerExact(session.name());
			if (online == null || !online.isOnline()) {
				continue;
			}
			syncSessionPosition(session.name(), online.getLocation());
			syncSessionInventory(session.name());
		}
	}

	public boolean canManage(Player requester, DummySession session) {
		return requester.hasPermission("dummy.admin") || requester.getUniqueId().equals(session.ownerUuid());
	}

	private String resolveName(Player requester, String requestedName) {
		String prefix = config.dummyNamePrefix() == null ? "" : config.dummyNamePrefix();
		if (requestedName != null && !requestedName.isBlank()) {
			String base = sanitizeName(requestedName.trim());
			if (prefix.isBlank()) {
				return base;
			}
			if (base.startsWith(prefix)) {
				return base;
			}
			return sanitizeName(prefix + base);
		}

		String generated = config.defaultNameTemplate()
				.replace("%player%", requester.getName())
				.replace("%uuid%", requester.getUniqueId().toString().substring(0, 8))
				.toLowerCase(Locale.ROOT);
		String value = sanitizeName(generated);
		if (prefix.isBlank() || value.startsWith(prefix)) {
			return value;
		}
		return sanitizeName(prefix + value);
	}

	private String sanitizeName(String name) {
		String value = name.replaceAll("[^A-Za-z0-9_]", "_");
		if (value.length() > 16) {
			value = value.substring(0, 16);
		}
		return value;
	}

	public record OperationResult(boolean success, String message, DummySession session, String warning) {
		public static OperationResult failed(String message) {
			return new OperationResult(false, message, null, null);
		}

		public static OperationResult success(DummySession session, String message, String warning) {
			return new OperationResult(true, message, session, warning);
		}
	}
}
