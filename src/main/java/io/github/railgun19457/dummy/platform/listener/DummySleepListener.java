package io.github.railgun19457.dummy.platform.listener;

import io.github.railgun19457.dummy.core.action.ActionType;
import io.github.railgun19457.dummy.core.model.DummyDefinition;
import io.github.railgun19457.dummy.core.model.DummyRuntimeState;
import io.github.railgun19457.dummy.core.port.CompatBridge;
import io.github.railgun19457.dummy.core.service.DummyLifecycleService;
import io.github.railgun19457.dummy.core.service.DummyQueryService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;

public final class DummySleepListener implements Listener {

    private final CompatBridge compatBridge;
    private final DummyLifecycleService lifecycleService;
    private final DummyQueryService queryService;

    public DummySleepListener(CompatBridge compatBridge, DummyLifecycleService lifecycleService, DummyQueryService queryService) {
        this.compatBridge = compatBridge;
        this.lifecycleService = lifecycleService;
        this.queryService = queryService;
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (!lifecycleService.followOwnerSleepEnabled() || event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) {
            return;
        }
        syncSleep(event.getPlayer(), true);
    }

    @EventHandler
    public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
        if (!lifecycleService.followOwnerSleepEnabled()) {
            return;
        }
        syncSleep(event.getPlayer(), false);
    }

    private void syncSleep(Player owner, boolean sleeping) {
        for (DummyDefinition definition : queryService.findByOwner(owner.getUniqueId())) {
            lifecycleService.resolveHandle(definition.id()).ifPresent(handle -> {
                if (sleeping) {
                    World world = Bukkit.getWorld(definition.lastKnownPosition().worldName());
                    if (world == null) {
                        return;
                    }
                    Location location = new Location(world, definition.lastKnownPosition().x(), definition.lastKnownPosition().y(), definition.lastKnownPosition().z());
                    compatBridge.sleep(handle, location);
                } else {
                    compatBridge.wakeUp(handle);
                }
                lifecycleService.updateRuntimeState(definition.id(), currentState -> new DummyRuntimeState(
                        currentState.dummyId(),
                        currentState.spawned(),
                        currentState.entityId(),
                        sleeping ? ActionType.SLEEP : ActionType.WAKE,
                        sleeping,
                        currentState.sneaking(),
                        currentState.sprinting(),
                        currentState.mountedTarget()
                ));
            });
        }
    }
}