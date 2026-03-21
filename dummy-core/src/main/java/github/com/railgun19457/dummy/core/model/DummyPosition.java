package github.com.railgun19457.dummy.core.model;

import org.bukkit.Location;

public record DummyPosition(
        String world,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) {
    public static DummyPosition from(Location location) {
        String worldName = location.getWorld() == null ? "world" : location.getWorld().getName();
        return new DummyPosition(worldName, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }
}
