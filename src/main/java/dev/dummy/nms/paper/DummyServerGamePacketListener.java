package dev.dummy.nms.paper;

import dev.dummy.DummyPlugin;
import dev.dummy.nms.paper.compat.PaperNmsCompatibility;
import java.util.Set;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

public final class DummyServerGamePacketListener extends ServerGamePacketListenerImpl {
    private static final Set<String> FORWARDED_PLUGIN_CHANNELS = Set.of("astrbot:proxy");

    private final DummyPlugin plugin;
    private final PaperNmsCompatibility nmsCompatibility;

    public DummyServerGamePacketListener(
            MinecraftServer server,
            Connection connection,
            ServerPlayer player,
            CommonListenerCookie cookie,
            DummyPlugin plugin,
            PaperNmsCompatibility nmsCompatibility
    ) {
        super(server, connection, player, cookie);
        this.plugin = plugin;
        this.nmsCompatibility = nmsCompatibility;
        this.pluginMessagerChannels.addAll(FORWARDED_PLUGIN_CHANNELS);
    }

    @Override
    public void tick() {
        // Fake players do not send movement packets. The vanilla connection tick
        // snaps players back to the last client-confirmed position every tick,
        // which prevents server-side gravity and knockback from taking effect.
    }

    @Override
    public void send(Packet<?> packet) {
        if (packet instanceof ClientboundCustomPayloadPacket payloadPacket && forwardPluginPayload(payloadPacket)) {
            return;
        }
        if (packet instanceof ClientboundSetEntityMotionPacket motionPacket) {
            handleMotionPacket(motionPacket);
        } else if (packet instanceof ClientboundPlayerPositionPacket positionPacket) {
            handlePositionPacket(positionPacket);
        }
    }

    private boolean forwardPluginPayload(ClientboundCustomPayloadPacket packet) {
        String channel = packet.payload().type().id().toString();
        if (!FORWARDED_PLUGIN_CHANNELS.contains(channel)) {
            return false;
        }
        CraftPlayer carrier = pluginMessageCarrier();
        if (carrier != null) {
            carrier.getHandle().connection.send(packet);
        }
        return true;
    }

    private CraftPlayer pluginMessageCarrier() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!(online instanceof CraftPlayer craftPlayer) || online.getUniqueId().equals(this.player.getUUID())) {
                continue;
            }
            if (craftPlayer.getHandle().connection == null || craftPlayer.getHandle().connection instanceof DummyServerGamePacketListener) {
                continue;
            }
            return craftPlayer;
        }
        return null;
    }

    private void handleMotionPacket(ClientboundSetEntityMotionPacket packet) {
        Integer entityId = nmsCompatibility.motionPacketEntityId(packet);
        if (entityId == null || entityId != this.player.getId()) {
            return;
        }
        Vec3 movement = nmsCompatibility.motionPacketMovement(packet);
        if (movement == null) {
            return;
        }
        runAfterCurrentTick(() -> {
            if (this.player.isRemoved()) {
                return;
            }
            this.player.setDeltaMovement(movement);
        });
    }

    private void handlePositionPacket(ClientboundPlayerPositionPacket packet) {
        runOnMain(() -> {
            handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(packet.id()));
            resetPosition();
        });
    }

    public void acceptPlayerLoad() {
        runOnMain(() -> {
            handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            resetPosition();
            resetFlyingTicks();
        });
    }

    public void closeDummyConnection() {
        if (this.connection instanceof DummyConnection dummyConnection) {
            dummyConnection.closeDummyConnection();
        }
    }

    private void runOnMain(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    private void runAfterCurrentTick(Runnable runnable) {
        // Player attacks send velocity to the client and then restore the server-side
        // ServerPlayer velocity. Fake players need the client velocity applied after that restore.
        Bukkit.getScheduler().runTask(plugin, runnable);
    }
}
