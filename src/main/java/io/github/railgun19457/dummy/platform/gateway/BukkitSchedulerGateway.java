package io.github.railgun19457.dummy.platform.gateway;

import io.github.railgun19457.dummy.core.port.SchedulerGateway;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitSchedulerGateway implements SchedulerGateway {

    private final JavaPlugin plugin;

    public BukkitSchedulerGateway(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runSync(Runnable runnable) {
        plugin.getServer().getScheduler().runTask(plugin, runnable);
    }

    @Override
    public void runAsync(Runnable runnable) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, runnable);
    }
}
