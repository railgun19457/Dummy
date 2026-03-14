package io.github.railgun19457.dummy.platform.listener;

import io.github.railgun19457.dummy.core.model.DummyDefinition;
import io.github.railgun19457.dummy.core.port.CompatBridge;
import io.github.railgun19457.dummy.core.service.DummyLifecycleService;
import io.github.railgun19457.dummy.core.service.DummyQueryService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.HashSet;
import java.util.Set;

public final class DummyChunkListener implements Listener {

    private final CompatBridge compatBridge;
    private final DummyQueryService queryService;
    private final DummyLifecycleService lifecycleService;

    public DummyChunkListener(CompatBridge compatBridge, DummyQueryService queryService, DummyLifecycleService lifecycleService) {
        this.compatBridge = compatBridge;
        this.queryService = queryService;
        this.lifecycleService = lifecycleService;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Set<io.github.railgun19457.dummy.core.model.DummyId> presentDummyIds = new HashSet<>();
        for (org.bukkit.entity.Entity entity : event.getChunk().getEntities()) {
            compatBridge.resolveDummyId(entity).ifPresent(presentDummyIds::add);
        }

        for (DummyDefinition definition : queryService.findAll()) {
            if (!definition.lastKnownPosition().worldName().equals(event.getWorld().getName())) {
                continue;
            }
            int chunkX = (int) Math.floor(definition.lastKnownPosition().x()) >> 4;
            int chunkZ = (int) Math.floor(definition.lastKnownPosition().z()) >> 4;
            if (chunkX != event.getChunk().getX() || chunkZ != event.getChunk().getZ()) {
                continue;
            }
            if (presentDummyIds.contains(definition.id())) {
                continue;
            }
            if (lifecycleService.runtimeState(definition.id()).map(state -> state.spawned()).orElse(false)) {
                lifecycleService.registerRecoveredDummy(definition, lifecycleService.runtimeState(definition.id()).orElseThrow());
            }
        }
    }
}
