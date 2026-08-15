package dev.dummy;

import dev.dummy.action.DummyActionService;
import dev.dummy.command.DummyCommand;
import dev.dummy.dummy.DummyManager;
import dev.dummy.dummy.DummyStorage;
import dev.dummy.gui.DummyGuiListener;
import dev.dummy.i18n.I18n;
import dev.dummy.listener.DummyExperienceListener;
import dev.dummy.listener.DummyLifecycleListener;
import dev.dummy.nms.FakePlayerAdapter;
import dev.dummy.nms.paper.PaperFakePlayerAdapter;
import dev.dummy.skin.SkinService;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class DummyPlugin extends JavaPlugin {
    private DummyManager dummyManager;
    private I18n i18n;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getMessenger().registerOutgoingPluginChannel(this, DummyManager.PROXY_TAB_CHANNEL);
        this.i18n = new I18n(this);
        this.i18n.saveDefaults();
        this.i18n.reload();

        try {
            FakePlayerAdapter adapter = new PaperFakePlayerAdapter(this);
            DummyStorage storage = new DummyStorage(this);
            this.dummyManager = new DummyManager(this, adapter, storage);
        } catch (RuntimeException ex) {
            getLogger().log(Level.SEVERE, "Failed to initialize Dummy NMS adapter", ex);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        SkinService skinService = new SkinService(this);
        DummyActionService actionService = new DummyActionService(this, dummyManager);
        dummyManager.setActionService(actionService);
        DummyGuiListener guiListener = new DummyGuiListener(this, dummyManager, i18n);
        DummyCommand dummyCommand = new DummyCommand(this, dummyManager, skinService, actionService, i18n, guiListener);
        registerCommand("dummy", "Manage dummy players.", List.of("dm"), dummyCommand);
        Bukkit.getPluginManager().registerEvents(new DummyExperienceListener(this, dummyManager), this);
        Bukkit.getPluginManager().registerEvents(new DummyLifecycleListener(this, dummyManager, actionService), this);
        Bukkit.getPluginManager().registerEvents(guiListener, this);

        if (getConfig().getBoolean("storage.restore-on-startup", true)) {
            Bukkit.getScheduler().runTask(this, () -> dummyManager.restoreSavedDummies());
        }
    }

    @Override
    public void onDisable() {
        if (dummyManager != null) {
            dummyManager.shutdown("plugin disabled");
        }
    }

    public void reloadDummyConfig() {
        reloadConfig();
        i18n.reload();
    }

    public I18n i18n() {
        return i18n;
    }
}
