package github.com.railgun19457.dummy.nms;

import github.com.railgun19457.dummy.api.model.DummySession;
import github.com.railgun19457.dummy.nms.action.ActionSetting;
import github.com.railgun19457.dummy.nms.action.ActionTicker;
import github.com.railgun19457.dummy.nms.action.ActionType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;

public interface NmsBridge {

    String spawnDummy(DummySession session);

    void removeDummy(DummySession session);

    @NotNull
    NMSEntity fromEntity(@NotNull Entity entity);

    @NotNull
    NMSServerPlayer fromPlayer(@NotNull Player player);

    @NotNull
    NMSNetwork createNetwork(@NotNull InetAddress address);

    @NotNull
    ActionTicker createActionTicker(@NotNull Player player, @NotNull ActionType type, @NotNull ActionSetting setting);

    boolean isSupported();
}
