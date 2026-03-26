package github.com.railgun19457.dummy;

import github.com.railgun19457.dummy.command.DummyCommand;
import github.com.railgun19457.dummy.common.config.PluginConfig;
import github.com.railgun19457.dummy.common.i18n.I18nService;
import github.com.railgun19457.dummy.core.manager.DummyManager;
import github.com.railgun19457.dummy.core.manager.DummyRegistry;
import github.com.railgun19457.dummy.common.log.PluginLogger;
import github.com.railgun19457.dummy.core.scheduler.ActionScheduler;
import github.com.railgun19457.dummy.nms.skin.SkinFetchService;
import github.com.railgun19457.dummy.core.storage.DummyStorageService;
import github.com.railgun19457.dummy.gui.DummyGuiService;
import github.com.railgun19457.dummy.listener.DummyInteractListener;
import github.com.railgun19457.dummy.nms.NmsBridge;
import github.com.railgun19457.dummy.nms.v1_21_1.NmsBridgeV1_21_11;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class DummyPlugin extends JavaPlugin {

    private PluginLogger pluginLogger;
    private DummyManager dummyManager;
    private SkinFetchService skinFetchService;
    private ActionScheduler actionScheduler;
    private I18nService i18nService;
    private DummyStorageService storageService;
    private PluginConfig pluginConfig;
    private DummyGuiService guiService;

    @Override
    public void onEnable() {
        this.pluginLogger = new PluginLogger(getLogger());
        saveDefaultConfig();

        this.pluginConfig = PluginConfig.from(getConfig());
        this.i18nService = new I18nService(this, getConfig().getString("language", "zh_CN"));
        this.skinFetchService = new SkinFetchService(pluginLogger, java.time.Duration.ofMinutes(pluginConfig.skinCacheTtlMinutes()));
        NmsBridge nmsBridge = new NmsBridgeV1_21_11(pluginLogger, skinFetchService);
        this.dummyManager = new DummyManager(pluginConfig, new DummyRegistry(), nmsBridge);
        this.actionScheduler = new ActionScheduler(this);
        this.storageService = new DummyStorageService(this, pluginLogger);
        this.guiService = new DummyGuiService();

        if (pluginConfig.saveData()) {
            var restored = storageService.loadSessions();
            getServer().getScheduler().runTaskLater(this, () -> {
                dummyManager.restoreSessions(restored, true);
                pluginLogger.info("[Storage] restored sessions: " + restored.size());
                getServer().getScheduler().runTaskLater(this, () -> {
                    for (var session : dummyManager.listAll()) {
                        dummyManager.applyStoredInventoryIfOnline(session.name());
                    }
                }, 10L);
            }, 2L);
        }

        registerCommands();
        registerListeners();

        pluginLogger.info("Dummy enabled. Version=" + getPluginMeta().getVersion());
    }

    @Override
    public void onDisable() {
        if (skinFetchService != null) {
            skinFetchService.shutdown();
        }
        if (actionScheduler != null) {
            actionScheduler.stopAll();
        }
        if (storageService != null && dummyManager != null && pluginConfig != null && pluginConfig.saveData()) {
            try {
                dummyManager.syncAllOnlineState();
                storageService.saveSessionsAsync(dummyManager.listAll()).get(5, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception exception) {
                pluginLogger.error("[Storage] save on disable failed", exception);
            }
            storageService.shutdown();
        }
        if (pluginLogger != null) {
            pluginLogger.info("Dummy disabled.");
        }
    }

    private void registerCommands() {
        PluginCommand command = getCommand("dummy");
        if (command == null) {
            throw new IllegalStateException("Command 'dummy' is not defined in plugin.yml");
        }

        DummyCommand executor = new DummyCommand(this, dummyManager, actionScheduler, guiService, i18nService);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    public void reloadRuntime() {
        reloadConfig();
        PluginConfig pluginConfig = PluginConfig.from(getConfig());
        this.i18nService = new I18nService(this, getConfig().getString("language", "zh_CN"));
        this.dummyManager.reloadConfig(pluginConfig);
        this.pluginConfig = pluginConfig;
    }

    public I18nService i18nService() {
        return i18nService;
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new DummyInteractListener(this, dummyManager, guiService), this);
    }
}
