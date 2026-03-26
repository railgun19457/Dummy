package github.com.railgun19457.dummy.core.util;

import github.com.railgun19457.dummy.api.model.DummyPosition;
import org.bukkit.Location;

public final class PositionUtil {

    private PositionUtil() {
    }

    public static DummyPosition from(Location location) {
        String worldName = location.getWorld() == null ? "world" : location.getWorld().getName();
        return new DummyPosition(worldName, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }
}
