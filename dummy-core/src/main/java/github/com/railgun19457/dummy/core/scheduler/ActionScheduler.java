package github.com.railgun19457.dummy.core.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class ActionScheduler {

    private final Plugin plugin;
    private final Map<UUID, RunningAction> runningActions = new ConcurrentHashMap<>();
    private final AtomicLong tickCounter = new AtomicLong(0);
    private BukkitTask dispatcherTask;

    public ActionScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    public void schedule(Player target, ActionTask task, ActionMode mode) {
        stop(target.getUniqueId());

        if (mode.type() == ActionMode.Type.ONCE) {
            Bukkit.getScheduler().runTask(plugin, () -> task.run(target));
            return;
        }

        long period = mode.type() == ActionMode.Type.CONTINUOUS ? 1L : mode.intervalTicks();
        runningActions.put(target.getUniqueId(), new RunningAction(target, task, period, tickCounter.get()));
        ensureDispatcherStarted();
    }

    public boolean stop(UUID playerId) {
        RunningAction removed = runningActions.remove(playerId);
        if (removed == null) {
            return false;
        }
        stopDispatcherIfIdle();
        return true;
    }

    public void stopAll() {
        runningActions.clear();
        if (dispatcherTask != null) {
            dispatcherTask.cancel();
            dispatcherTask = null;
        }
    }

    private void ensureDispatcherStarted() {
        if (dispatcherTask != null && !dispatcherTask.isCancelled()) {
            return;
        }
        dispatcherTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long tick = tickCounter.incrementAndGet();
            ArrayList<UUID> toRemove = new ArrayList<>();
            for (Map.Entry<UUID, RunningAction> entry : runningActions.entrySet()) {
                RunningAction action = entry.getValue();
                Player target = action.target();
                if (target == null || !target.isOnline()) {
                    toRemove.add(entry.getKey());
                    continue;
                }
                if (tick < action.nextRunTick()) {
                    continue;
                }
                action.task().run(target);
                entry.setValue(action.next(tick + action.period()));
            }
            for (UUID uuid : toRemove) {
                runningActions.remove(uuid);
            }
            stopDispatcherIfIdle();
        }, 1L, 1L);
    }

    private void stopDispatcherIfIdle() {
        if (!runningActions.isEmpty()) {
            return;
        }
        if (dispatcherTask != null) {
            dispatcherTask.cancel();
            dispatcherTask = null;
        }
    }

    private record RunningAction(Player target, ActionTask task, long period, long nextRunTick) {
        private RunningAction next(long nextRunTick) {
            return new RunningAction(target, task, period, nextRunTick);
        }
    }
}
