package io.github.railgun19457.dummy.platform.listener;

import io.github.railgun19457.dummy.core.port.CompatBridge;
import io.github.railgun19457.dummy.core.service.DummyQueryService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public final class DummyDamageListener implements Listener {

    private final CompatBridge compatBridge;
    private final DummyQueryService queryService;

    public DummyDamageListener(CompatBridge compatBridge, DummyQueryService queryService) {
        this.compatBridge = compatBridge;
        this.queryService = queryService;
    }

    @EventHandler
    public void onDummyDamage(EntityDamageEvent event) {
        compatBridge.resolveDummyId(event.getEntity())
                .flatMap(queryService::findById)
                .ifPresent(definition -> event.setCancelled(definition.traitSet().invulnerable()));
    }
}
