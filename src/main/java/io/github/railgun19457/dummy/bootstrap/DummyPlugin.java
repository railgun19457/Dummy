package io.github.railgun19457.dummy.bootstrap;

import io.github.railgun19457.dummy.common.config.PluginConfig;
import io.github.railgun19457.dummy.common.config.PluginConfigLoader;
import io.github.railgun19457.dummy.compat.v120x.V120xCompatBridge;
import io.github.railgun19457.dummy.compat.v121x.V121xCompatBridge;
import io.github.railgun19457.dummy.core.action.builtin.AttackAction;
import io.github.railgun19457.dummy.core.action.builtin.DigAction;
import io.github.railgun19457.dummy.core.action.builtin.DropAction;
import io.github.railgun19457.dummy.core.action.builtin.ExecuteCommandAction;
import io.github.railgun19457.dummy.core.action.builtin.HoldItemAction;
import io.github.railgun19457.dummy.core.action.builtin.JumpAction;
import io.github.railgun19457.dummy.core.action.builtin.LookAction;
import io.github.railgun19457.dummy.core.action.builtin.LookAtEntityAction;
import io.github.railgun19457.dummy.core.action.builtin.MoveAction;
import io.github.railgun19457.dummy.core.action.builtin.RunAction;
import io.github.railgun19457.dummy.core.action.builtin.RideAction;
import io.github.railgun19457.dummy.core.action.builtin.SendChatAction;
import io.github.railgun19457.dummy.core.action.builtin.SneakAction;
import io.github.railgun19457.dummy.core.action.builtin.SleepAction;
import io.github.railgun19457.dummy.core.action.builtin.SwapHandAction;
import io.github.railgun19457.dummy.core.action.builtin.UseAction;
import io.github.railgun19457.dummy.core.action.builtin.WakeAction;
import io.github.railgun19457.dummy.core.action.DummyActionRegistry;
import io.github.railgun19457.dummy.core.port.CompatBridge;
import io.github.railgun19457.dummy.core.port.DummyRepository;
import io.github.railgun19457.dummy.core.service.DummyControlService;
import io.github.railgun19457.dummy.core.service.DummyLifecycleService;
import io.github.railgun19457.dummy.core.service.DummyOwnershipService;
import io.github.railgun19457.dummy.core.service.DummyQueryService;
import io.github.railgun19457.dummy.core.service.DummyRecoveryService;
import io.github.railgun19457.dummy.i18n.MessageBundleRegistry;
import io.github.railgun19457.dummy.persistence.memory.InMemoryDummyRepository;
import io.github.railgun19457.dummy.persistence.sqlite.SqliteDatabase;
import io.github.railgun19457.dummy.persistence.sqlite.SqliteDummyRepository;
import io.github.railgun19457.dummy.platform.gateway.BukkitMessageGateway;
import io.github.railgun19457.dummy.platform.gateway.BukkitPermissionGateway;
import io.github.railgun19457.dummy.platform.gateway.OfflineSkinGateway;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class DummyPlugin extends JavaPlugin {

    private BootstrapContext bootstrapContext;
    private SqliteDatabase sqliteDatabase;
    private DummyLifecycleService lifecycleService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String minecraftVersion = Bukkit.getMinecraftVersion();
        String platform = PlatformResolver.resolvePlatformName();
        boolean paperServer = PlatformResolver.isPaperServer();
        String compatVersion = VersionResolver.resolveCompatVersion(minecraftVersion);
        PluginConfig pluginConfig = PluginConfigLoader.load(this);
        MessageBundleRegistry messageBundleRegistry = MessageBundleRegistry.load(this, pluginConfig.defaultLocale());

        this.sqliteDatabase = new SqliteDatabase(this);
        this.sqliteDatabase.initialize();

        CompatBridge compatBridge = switch (compatVersion) {
            case "v120x" -> new V120xCompatBridge(this);
            case "v121x" -> new V121xCompatBridge(this);
            default -> throw new IllegalStateException("Unsupported compat version: " + compatVersion);
        };
        DummyRepository dummyRepository = pluginConfig.persistence().saveDummyData()
            ? new SqliteDummyRepository(sqliteDatabase)
            : new InMemoryDummyRepository();
        BukkitMessageGateway messageGateway = new BukkitMessageGateway(messageBundleRegistry, pluginConfig.defaultLocale());
        DummyOwnershipService ownershipService = new DummyOwnershipService(new BukkitPermissionGateway());
        DummyQueryService queryService = new DummyQueryService(dummyRepository);
        this.lifecycleService = new DummyLifecycleService(
            pluginConfig,
            dummyRepository,
            compatBridge,
            new OfflineSkinGateway()
        );
        DummyRecoveryService recoveryService = new DummyRecoveryService(pluginConfig, dummyRepository, compatBridge, lifecycleService);
        DummyActionRegistry actionRegistry = new DummyActionRegistry(List.of(
            new DropAction(compatBridge, lifecycleService),
            new UseAction(compatBridge),
            new SleepAction(compatBridge, lifecycleService, queryService),
            new WakeAction(compatBridge, lifecycleService),
            new AttackAction(compatBridge),
            new DigAction(compatBridge, queryService),
            new JumpAction(compatBridge),
            new SwapHandAction(compatBridge, lifecycleService),
            new LookAction(compatBridge),
            new LookAtEntityAction(compatBridge),
            new MoveAction(lifecycleService, queryService),
            new SneakAction(compatBridge, lifecycleService),
            new RunAction(compatBridge, lifecycleService),
            new RideAction(compatBridge, lifecycleService),
            new HoldItemAction(compatBridge, lifecycleService),
            new ExecuteCommandAction(queryService),
            new SendChatAction(queryService)
        ));
        DummyControlService controlService = new DummyControlService(actionRegistry, lifecycleService);

        this.bootstrapContext = new BootstrapContext(
                this,
                platform,
                minecraftVersion,
                paperServer,
                pluginConfig,
                messageBundleRegistry
        );

            new ComponentRegistrar(this).registerPlatformComponents(
                compatBridge,
                lifecycleService,
                controlService,
                queryService,
                ownershipService,
                messageGateway
            );
            lifecycleService.restoreStoredExperience(dummyRepository.findAllStoredExperience());
            recoveryService.recoverAll();

        getLogger().info(messageBundleRegistry.getMessage(pluginConfig.defaultLocale(), "dummy.bootstrap.enabled")
                + " -> " + platform + " (MC " + minecraftVersion + ", compat " + compatVersion + ")");
    }

    @Override
    public void onDisable() {
        if (bootstrapContext != null) {
            getLogger().info(bootstrapContext.messageBundleRegistry().getMessage(
                    bootstrapContext.pluginConfig().defaultLocale(),
                    "dummy.bootstrap.disabled"
            ));
        }
        if (lifecycleService != null) {
            lifecycleService.shutdownAll();
            lifecycleService = null;
        }
        if (sqliteDatabase != null) {
            sqliteDatabase.close();
            sqliteDatabase = null;
        }
        this.bootstrapContext = null;
    }

    public BootstrapContext bootstrapContext() {
        return bootstrapContext;
    }
}
