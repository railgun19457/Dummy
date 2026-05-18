package dev.dummy.nms.paper;

import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public final class DummyTicker extends BukkitRunnable {
    private final ServerPlayer handle;
    private boolean firstTick = true;

    public DummyTicker(ServerPlayer handle) {
        this.handle = handle;
    }

    @Override
    public void run() {
        if (!handle.getBukkitEntity().isOnline() || handle.isRemoved()) {
            cancel();
            return;
        }

        if (firstTick) {
            doFirstTick();
            firstTick = false;
            return;
        }

        handle.doTick();
    }

    private void doFirstTick() {
        double x = handle.getX();
        double y = handle.getY();
        double z = handle.getZ();
        float yaw = handle.getYRot();
        float pitch = handle.getXRot();

        handle.xo = x;
        handle.yo = y;
        handle.zo = z;
        handle.doTick();

        Player player = handle.getBukkitEntity();
        player.teleport(new Location(player.getWorld(), x, y, z, yaw, pitch));
        handle.absSnapTo(x, y, z, yaw, pitch);
    }
}
