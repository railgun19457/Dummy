package github.com.railgun19457.dummy.nms.v1_21_11;

import com.mojang.authlib.GameProfile;
import github.com.railgun19457.dummy.core.log.PluginLogger;
import github.com.railgun19457.dummy.core.model.DummySession;
import github.com.railgun19457.dummy.core.skin.SkinFetchService;
import github.com.railgun19457.dummy.core.skin.SkinTexture;
import github.com.railgun19457.dummy.nms.v1_21_11.network.DummyConnection;
import github.com.railgun19457.dummy.nms.v1_21_11.network.DummyServerGamePacketListener;
import github.com.railgun19457.dummy.nms.NmsBridge;
import com.mojang.authlib.properties.Property;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NmsBridgeV1_21_11 implements NmsBridge {

	private final PluginLogger logger;
	private final SkinFetchService skinFetchService;
	private final Map<UUID, ServerPlayer> activePlayers = new ConcurrentHashMap<>();

	public NmsBridgeV1_21_11(PluginLogger logger, SkinFetchService skinFetchService) {
		this.logger = logger;
		this.skinFetchService = skinFetchService;
	}

	@Override
	public String spawnDummy(DummySession session) {
		try {
			MinecraftServer server = getMinecraftServer();
			ServerLevel level = getServerLevel(session);
			GameProfile profile = new GameProfile(session.id(), session.name());

			String skinSource = (session.skinSource() == null || session.skinSource().isBlank()) ? session.ownerName() : session.skinSource();
			SkinTexture skinTexture = skinFetchService.getCached(skinSource).orElse(null);
			if (skinTexture != null) {
				applyTextureProperty(profile, skinTexture);
			} else {
				skinFetchService.fetchAsyncIfAbsent(skinSource);
			}

			ServerPlayer serverPlayer = new ServerPlayer(
					server,
					level,
					profile,
					ClientInformation.createDefault()
			);

			var location = session.position();
			moveServerPlayer(serverPlayer,
					location.x(),
					location.y(),
					location.z(),
					location.yaw(),
					location.pitch()
			);

			DummyConnection connection = new DummyConnection(InetAddress.getLoopbackAddress());
			CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
			server.getPlayerList().placeNewPlayer(connection, serverPlayer, cookie);

			DummyServerGamePacketListener listener = new DummyServerGamePacketListener(server, connection, serverPlayer, cookie);
			serverPlayer.connection = listener;

			var bukkitEntity = serverPlayer.getBukkitEntity();
			if (bukkitEntity != null && session.traits() != null) {
				bukkitEntity.setCollidable(session.traits().collision());
				bukkitEntity.setInvulnerable(session.traits().invulnerable());
			}

			activePlayers.put(session.id(), serverPlayer);
			logger.info("[NMS] spawned dummy: " + session.name());
			if (skinTexture == null) {
				return "皮肤纹理正在异步拉取并缓存，首次生成可能显示默认皮肤。";
			}
			return null;
		} catch (Throwable exception) {
			logger.error("[NMS] failed to spawn dummy: " + session.name(), exception);
			return "NMS 生成失败: " + exception.getMessage();
		}
	}

	@Override
	public void removeDummy(DummySession session) {
		try {
			ServerPlayer serverPlayer = activePlayers.remove(session.id());
			if (serverPlayer == null) {
				return;
			}

			MinecraftServer server = getMinecraftServer();
			removeFromPlayerList(server, serverPlayer);
			logger.info("[NMS] removed dummy: " + session.name());
		} catch (Throwable exception) {
			logger.error("[NMS] failed to remove dummy: " + session.name(), exception);
		}
	}

	private void applyTextureProperty(GameProfile profile, SkinTexture skinTexture) {
		Property property = new Property("textures", skinTexture.value(), skinTexture.signature());
		try {
			Method getProperties = profile.getClass().getMethod("getProperties");
			Object propertyMap = getProperties.invoke(profile);
			Method put = propertyMap.getClass().getMethod("put", Object.class, Object.class);
			put.invoke(propertyMap, "textures", property);
			return;
		} catch (Throwable ignored) {
		}

		try {
			Method properties = profile.getClass().getMethod("properties");
			Object propertyMap = properties.invoke(profile);
			Method put = propertyMap.getClass().getMethod("put", Object.class, Object.class);
			put.invoke(propertyMap, "textures", property);
		} catch (Throwable exception) {
			logger.warn("[NMS] skip skin property apply due to authlib api mismatch: " + exception.getClass().getSimpleName());
		}
	}

	private void removeFromPlayerList(MinecraftServer server, ServerPlayer serverPlayer) throws ReflectiveOperationException {
		Object playerList = server.getPlayerList();
		Method removeMethod = playerList.getClass().getMethod("remove", ServerPlayer.class);
		removeMethod.invoke(playerList, serverPlayer);
	}

	private void moveServerPlayer(ServerPlayer serverPlayer, double x, double y, double z, float yaw, float pitch) throws ReflectiveOperationException {
		try {
			Method absMoveTo = ServerPlayer.class.getMethod("absMoveTo", double.class, double.class, double.class, float.class, float.class);
			absMoveTo.invoke(serverPlayer, x, y, z, yaw, pitch);
			return;
		} catch (NoSuchMethodException ignored) {
		}

		try {
			Method moveTo = ServerPlayer.class.getMethod("moveTo", double.class, double.class, double.class, float.class, float.class);
			moveTo.invoke(serverPlayer, x, y, z, yaw, pitch);
			return;
		} catch (NoSuchMethodException ignored) {
		}

		Method setPos = ServerPlayer.class.getMethod("setPos", double.class, double.class, double.class);
		setPos.invoke(serverPlayer, x, y, z);
		Method setYRot = ServerPlayer.class.getMethod("setYRot", float.class);
		Method setXRot = ServerPlayer.class.getMethod("setXRot", float.class);
		setYRot.invoke(serverPlayer, yaw);
		setXRot.invoke(serverPlayer, pitch);
	}

	private MinecraftServer getMinecraftServer() throws ReflectiveOperationException {
		Object craftServer = Bukkit.getServer();
		Method getServer = craftServer.getClass().getMethod("getServer");
		return (MinecraftServer) getServer.invoke(craftServer);
	}

	private ServerLevel getServerLevel(DummySession session) throws ReflectiveOperationException {
		World world = session.position() == null ? Bukkit.getWorlds().getFirst() : Bukkit.getWorld(session.position().world());
		if (world == null) {
			world = Bukkit.getWorlds().getFirst();
		}
		Object craftWorld = world;
		Method getHandle = craftWorld.getClass().getMethod("getHandle");
		return (ServerLevel) getHandle.invoke(craftWorld);
	}
}
