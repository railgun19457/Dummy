package io.github.railgun19457.dummy.bootstrap;

import io.github.railgun19457.dummy.core.port.MessageGateway;
import io.github.railgun19457.dummy.core.port.CompatBridge;
import io.github.railgun19457.dummy.core.service.DummyControlService;
import io.github.railgun19457.dummy.core.service.DummyLifecycleService;
import io.github.railgun19457.dummy.core.service.DummyOwnershipService;
import io.github.railgun19457.dummy.core.service.DummyQueryService;
import io.github.railgun19457.dummy.platform.command.DummyCommand;
import io.github.railgun19457.dummy.platform.listener.DummyChunkListener;
import io.github.railgun19457.dummy.platform.listener.DummyDamageListener;
import io.github.railgun19457.dummy.platform.listener.DummyExperienceListener;
import io.github.railgun19457.dummy.platform.listener.DummyInventoryListener;
import io.github.railgun19457.dummy.platform.listener.DummySleepListener;
import io.github.railgun19457.dummy.platform.listener.PlayerInteractDummyListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class ComponentRegistrar {

    private final JavaPlugin plugin;

    public ComponentRegistrar(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerPlatformComponents(
            CompatBridge compatBridge,
            DummyLifecycleService lifecycleService,
            DummyControlService controlService,
            DummyQueryService queryService,
            DummyOwnershipService ownershipService,
            MessageGateway messageGateway
    ) {
        PluginCommand command = plugin.getCommand("dummy");
        if (command == null) {
            throw new IllegalStateException("Command /dummy is not defined in plugin.yml");
        }
        DummyCommand dummyCommand = new DummyCommand(lifecycleService, controlService, queryService, ownershipService, messageGateway);
        command.setExecutor(dummyCommand);
        command.setTabCompleter(dummyCommand);

        plugin.getServer().getPluginManager().registerEvents(new PlayerInteractDummyListener(compatBridge, queryService), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DummyDamageListener(compatBridge, queryService), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DummyExperienceListener(compatBridge, lifecycleService), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DummyInventoryListener(queryService, lifecycleService), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DummySleepListener(compatBridge, lifecycleService, queryService), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DummyChunkListener(compatBridge, queryService, lifecycleService), plugin);
    }
}
