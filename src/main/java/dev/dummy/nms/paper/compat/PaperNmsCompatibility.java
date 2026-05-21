package dev.dummy.nms.paper.compat;

import dev.dummy.DummyPlugin;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;

public interface PaperNmsCompatibility {
    static PaperNmsCompatibility detect(DummyPlugin plugin) {
        String minecraftVersion = Bukkit.getMinecraftVersion();
        PaperNmsCompatibility compatibility = minecraftVersion.startsWith("26.")
                ? new Paper2612NmsCompatibility()
                : new Paper12111NmsCompatibility();
        plugin.getLogger().info("Using " + compatibility.name() + " NMS compatibility for Minecraft " + minecraftVersion);
        return compatibility;
    }

    String name();

    Integer motionPacketEntityId(ClientboundSetEntityMotionPacket packet);

    Vec3 motionPacketMovement(ClientboundSetEntityMotionPacket packet);

    default boolean removeTrackedViewer(ServerPlayer tracked, ServerPlayer viewer) {
        ChunkMap.TrackedEntity entry = trackedEntity(tracked);
        if (entry == null) {
            return false;
        }
        entry.removePlayer(viewer);
        return true;
    }

    default boolean updateTrackedViewer(ServerPlayer tracked, ServerPlayer viewer) {
        ChunkMap.TrackedEntity entry = trackedEntity(tracked);
        if (entry == null) {
            return false;
        }
        entry.updatePlayer(viewer);
        return true;
    }

    private ChunkMap.TrackedEntity trackedEntity(ServerPlayer tracked) {
        return tracked.level().getChunkSource().chunkMap.entityMap.get(tracked.getId());
    }
}
