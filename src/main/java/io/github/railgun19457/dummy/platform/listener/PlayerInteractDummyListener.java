package io.github.railgun19457.dummy.platform.listener;

import io.github.railgun19457.dummy.core.port.CompatBridge;
import io.github.railgun19457.dummy.core.service.DummyQueryService;
import io.github.railgun19457.dummy.platform.gui.DummyInventoryView;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

public final class PlayerInteractDummyListener implements Listener {

    private final CompatBridge compatBridge;
    private final DummyQueryService queryService;

    public PlayerInteractDummyListener(CompatBridge compatBridge, DummyQueryService queryService) {
        this.compatBridge = compatBridge;
        this.queryService = queryService;
    }

    @EventHandler
    public void onPlayerInteractDummy(PlayerInteractAtEntityEvent event) {
        compatBridge.resolveDummyId(event.getRightClicked())
                .flatMap(queryService::findById)
                .ifPresent(definition -> {
                    event.setCancelled(true);
                    if (event.getPlayer().isSneaking() && definition.traitSet().allowInteractionConfigure()) {
                        DummyInventoryView.openConfig(event.getPlayer(), definition);
                        return;
                    }
                    if (definition.traitSet().allowInventoryOpen()) {
                        DummyInventoryView.open(event.getPlayer(), definition);
                    }
                });
    }
}
