package github.com.railgun19457.dummy.nms.v1_21_1.action;

import github.com.railgun19457.dummy.nms.NMSServerPlayer;
import github.com.railgun19457.dummy.nms.action.*;
import org.jetbrains.annotations.NotNull;

public abstract class BaseActionTicker implements ActionTicker {

    protected Action action;
    protected ActionSetting setting;

    public BaseActionTicker(@NotNull NMSServerPlayer player, @NotNull ActionType type, @NotNull ActionSetting setting) {
        this.setting = setting;
        this.action = createAction(player, type);
    }

    protected abstract Action createAction(@NotNull NMSServerPlayer player, @NotNull ActionType type);

    @Override
    @NotNull
    public ActionSetting getSetting() {
        return setting;
    }

    @Override
    public boolean tick() {
        if (this.setting.equals(ActionSetting.stop())) {
            this.action.stop();
            return true;
        }

        if (setting.wait > 0) {
            this.setting.wait--;
            this.inactiveTick();
            return false;
        }

        if (this.setting.remains == 0) {
            this.inactiveTick();
            return true;
        }

        try {
            if (this.action.tick()) {
                if (this.setting.remains > 0) {
                    this.setting.remains--;
                }
            }
        } finally {
            this.setting.wait = this.setting.interval;
        }

        return false;
    }

    @Override
    public void inactiveTick() {
        action.inactiveTick();
    }

    @Override
    public void stop() {
        action.stop();
    }
}
