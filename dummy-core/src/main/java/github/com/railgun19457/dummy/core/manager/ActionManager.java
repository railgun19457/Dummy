package github.com.railgun19457.dummy.core.manager;

import github.com.railgun19457.dummy.nms.NmsBridge;
import github.com.railgun19457.dummy.nms.action.ActionSetting;
import github.com.railgun19457.dummy.nms.action.ActionTicker;
import github.com.railgun19457.dummy.nms.action.ActionType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ActionManager {

    private final NmsBridge nmsBridge;
    private final Map<UUID, ActionTicker> activeActions = new ConcurrentHashMap<>();

    public ActionManager(NmsBridge nmsBridge) {
        this.nmsBridge = nmsBridge;
    }

    public void startAction(@NotNull Player player, @NotNull ActionType type, @NotNull ActionSetting setting) {
        stopAction(player);
        ActionTicker ticker = nmsBridge.createActionTicker(player, type, setting);
        activeActions.put(player.getUniqueId(), ticker);
    }

    public void stopAction(@NotNull Player player) {
        ActionTicker ticker = activeActions.remove(player.getUniqueId());
        if (ticker != null) {
            ticker.stop();
        }
    }

    @Nullable
    public ActionTicker getAction(@NotNull Player player) {
        return activeActions.get(player.getUniqueId());
    }

    public void tick() {
        activeActions.entrySet().removeIf(entry -> {
            ActionTicker ticker = entry.getValue();
            return ticker.tick();
        });
    }

    public void clear() {
        activeActions.values().forEach(ActionTicker::stop);
        activeActions.clear();
    }
}
