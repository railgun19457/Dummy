package dev.dummy.listener;

import dev.dummy.DummyPlugin;
import dev.dummy.action.DummyActionService;
import dev.dummy.dummy.DummyInstance;
import dev.dummy.dummy.DummyManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class DummyLifecycleListener implements Listener {
    private static final long JOIN_DUMMY_SYNC_DELAY_TICKS = 20L;

    private final DummyPlugin plugin;
    private final DummyManager dummyManager;
    private final DummyActionService actionService;

    public DummyLifecycleListener(DummyPlugin plugin, DummyManager dummyManager, DummyActionService actionService) {
        this.plugin = plugin;
        this.dummyManager = dummyManager;
        this.actionService = actionService;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (dummyManager.isDummy(event.getPlayer())) {
            DummyInstance dummy = dummyManager.get(event.getPlayer().getUniqueId());
            if (dummy != null && !actionService.preserveOnLifecycle()) {
                actionService.stop(dummy, null);
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> dummyManager.handleDeath(event.getPlayer()));
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline() && !dummyManager.isDummy(event.getPlayer())) {
                dummyManager.applyTabVisibility(event.getPlayer());
                dummyManager.syncProxyTab();
            }
        }, JOIN_DUMMY_SYNC_DELAY_TICKS);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (dummyManager.isDummy(event.getPlayer())) {
            DummyInstance dummy = dummyManager.get(event.getPlayer().getUniqueId());
            if (dummy != null && !actionService.preserveOnLifecycle()) {
                actionService.stop(dummy, null);
            }
            dummyManager.cleanup(event.getPlayer());
        }
    }
}
