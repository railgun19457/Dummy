package github.com.railgun19457.dummy.nms.v1_21_1.action;

import github.com.railgun19457.dummy.nms.NMSServerPlayer;
import github.com.railgun19457.dummy.nms.action.Action;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class LookAtEntityAction implements Action {

    private final NMSServerPlayer player;

    public LookAtEntityAction(@NotNull NMSServerPlayer player) {
        this.player = player;
    }

    @Override
    public boolean tick() {
        Entity nearest = player.getPlayer().getNearbyEntities(10, 10, 10).stream()
                .filter(e -> e != player.getPlayer())
                .min((e1, e2) -> Double.compare(
                        e1.getLocation().distanceSquared(player.getPlayer().getLocation()),
                        e2.getLocation().distanceSquared(player.getPlayer().getLocation())
                ))
                .orElse(null);

        if (nearest != null) {
            var loc = nearest.getLocation();
            var pLoc = player.getPlayer().getEyeLocation();
            double dx = loc.getX() - pLoc.getX();
            double dy = loc.getY() - pLoc.getY();
            double dz = loc.getZ() - pLoc.getZ();

            double distance = Math.sqrt(dx * dx + dz * dz);
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float pitch = (float) Math.toDegrees(Math.atan2(-dy, distance));

            player.setYRot(yaw);
            player.setXRot(pitch);
            player.resetLastActionTime();
            return true;
        }
        return false;
    }

    @Override
    public void inactiveTick() {
    }

    @Override
    public void stop() {
    }
}
