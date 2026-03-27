package github.com.railgun19457.dummy.nms.action;

import org.jetbrains.annotations.NotNull;

public interface ActionTicker {

    @NotNull
    ActionSetting getSetting();

    boolean tick();

    void inactiveTick();

    void stop();
}
