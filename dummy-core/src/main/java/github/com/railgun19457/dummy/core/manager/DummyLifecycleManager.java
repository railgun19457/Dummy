package github.com.railgun19457.dummy.core.manager;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DummyLifecycleManager {

    private final Map<UUID, LifecycleData> lifecycles = new ConcurrentHashMap<>();

    public void register(@NotNull Player player, long lifespanTicks) {
        lifecycles.put(player.getUniqueId(), new LifecycleData(System.currentTimeMillis(), lifespanTicks));
    }

    public void unregister(@NotNull Player player) {
        lifecycles.remove(player.getUniqueId());
    }

    public boolean shouldRemove(@NotNull Player player) {
        LifecycleData data = lifecycles.get(player.getUniqueId());
        if (data == null || data.lifespanTicks <= 0) {
            return false;
        }
        long elapsed = (System.currentTimeMillis() - data.spawnTime) / 50;
        return elapsed >= data.lifespanTicks;
    }

    public void clear() {
        lifecycles.clear();
    }

    private static class LifecycleData {
        final long spawnTime;
        final long lifespanTicks;

        LifecycleData(long spawnTime, long lifespanTicks) {
            this.spawnTime = spawnTime;
            this.lifespanTicks = lifespanTicks;
        }
    }
}
