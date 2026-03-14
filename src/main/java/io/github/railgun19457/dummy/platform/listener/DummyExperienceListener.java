package io.github.railgun19457.dummy.platform.listener;

import io.github.railgun19457.dummy.core.port.CompatBridge;
import io.github.railgun19457.dummy.core.service.DummyLifecycleService;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DummyExperienceListener implements Listener {

    private final CompatBridge compatBridge;
    private final DummyLifecycleService lifecycleService;
    private final Map<UUID, io.github.railgun19457.dummy.core.model.DummyId> lastDamager = new ConcurrentHashMap<>();

    public DummyExperienceListener(CompatBridge compatBridge, DummyLifecycleService lifecycleService) {
        this.compatBridge = compatBridge;
        this.lifecycleService = lifecycleService;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        compatBridge.resolveDummyId(event.getDamager())
                .ifPresent(dummyId -> lastDamager.put(event.getEntity().getUniqueId(), dummyId));
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        io.github.railgun19457.dummy.core.model.DummyId dummyId = lastDamager.remove(entity.getUniqueId());
        if (dummyId != null && event.getDroppedExp() > 0) {
            lifecycleService.addExperience(dummyId, event.getDroppedExp());
        }
    }
}
