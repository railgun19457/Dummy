package github.com.railgun19457.dummy.core.task;

import github.com.railgun19457.dummy.core.manager.ActionManager;
import github.com.railgun19457.dummy.core.manager.DummyTickerManager;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class DummyTickTask extends BukkitRunnable {

    private final DummyTickerManager tickerManager;
    private final ActionManager actionManager;

    public DummyTickTask(DummyTickerManager tickerManager, ActionManager actionManager) {
        this.tickerManager = tickerManager;
        this.actionManager = actionManager;
    }

    @Override
    public void run() {
        tickerManager.tick();
        actionManager.tick();
    }

    public void start(Plugin plugin) {
        this.runTaskTimer(plugin, 1L, 1L);
    }
}
