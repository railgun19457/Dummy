package dev.dummy.nms.paper;

import com.mojang.authlib.GameProfile;
import dev.dummy.DummyPlugin;
import dev.dummy.dummy.DummyCreateRequest;
import dev.dummy.i18n.LocalizedException;
import dev.dummy.nms.DummyHandle;
import dev.dummy.nms.FakePlayerAdapter;
import dev.dummy.nms.paper.compat.PaperNmsCompatibility;
import java.net.InetAddress;
import java.net.UnknownHostException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public final class PaperFakePlayerAdapter implements FakePlayerAdapter {
    private final DummyPlugin plugin;
    private final PaperNmsCompatibility nmsCompatibility;

    public PaperFakePlayerAdapter(DummyPlugin plugin) {
        this.plugin = plugin;
        this.nmsCompatibility = PaperNmsCompatibility.detect(plugin);
    }

    @Override
    public DummyHandle spawn(DummyCreateRequest request) {
        Location location = request.location();
        if (!(location.getWorld() instanceof CraftWorld craftWorld)) {
            throw new LocalizedException("error.craft-world-required");
        }

        CraftServer craftServer = (CraftServer) Bukkit.getServer();
        MinecraftServer server = craftServer.getServer();
        ServerLevel level = craftWorld.getHandle();
        GameProfile profile = PaperSkinSupport.createProfile(request.uuid(), request.name(), request.skin());
        ServerPlayer serverPlayer = new ServerPlayer(server, level, profile, PaperSkinSupport.withAllModelParts(ClientInformation.createDefault()));
        serverPlayer.absSnapTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

        Player player = serverPlayer.getBukkitEntity();
        player.setSleepingIgnored(true);
        player.displayName(Component.text(request.name(), NamedTextColor.GRAY));

        DummyConnection connection = new DummyConnection(addressFor(request.name()));
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(((CraftPlayer) player).getProfile(), false);
        craftServer.getHandle().placeNewPlayer(connection, serverPlayer, cookie);

        DummyServerGamePacketListener listener = new DummyServerGamePacketListener(server, connection, serverPlayer, cookie, plugin, nmsCompatibility);
        connection.setupInboundProtocol(
                GameProtocols.SERVERBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(server.registryAccess()), listener),
                listener
        );
        serverPlayer.connection = listener;
        player.teleport(location);
        resetSurvivalState(player);
        listener.acceptPlayerLoad();

        DummyTicker ticker = new DummyTicker(serverPlayer);
        BukkitTask tickerTask = ticker.runTaskTimer(plugin, 0L, 1L);
        PaperDummyHandle handle = new PaperDummyHandle(plugin, serverPlayer, tickerTask);
        handle.applySettings(request.name(), request.settings());
        return handle;
    }

    private InetAddress addressFor(String name) {
        int hash = Math.abs(name.hashCode());
        byte[] address = new byte[]{10, (byte) (hash >>> 16), (byte) (hash >>> 8), (byte) hash};
        try {
            return InetAddress.getByAddress(address);
        } catch (UnknownHostException ex) {
            return InetAddress.getLoopbackAddress();
        }
    }

    private void resetSurvivalState(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        player.setHealth(maxHealth == null ? 20.0D : maxHealth.getValue());
        player.setFoodLevel(20);
        player.setSaturation(5.0F);
        player.setFireTicks(0);
    }
}
