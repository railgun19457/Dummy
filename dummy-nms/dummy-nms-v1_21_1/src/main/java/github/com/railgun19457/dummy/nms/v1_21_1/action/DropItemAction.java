package github.com.railgun19457.dummy.nms.v1_21_1.action;

import github.com.railgun19457.dummy.nms.NMSServerPlayer;
import github.com.railgun19457.dummy.nms.action.Action;
import org.jetbrains.annotations.NotNull;

public class DropItemAction implements Action {

    private final NMSServerPlayer player;

    public DropItemAction(@NotNull NMSServerPlayer player) {
        this.player = player;
    }

    @Override
    public boolean tick() {
        player.getPlayer().dropItem(false);
        player.resetLastActionTime();
        return true;
    }

    @Override
    public void inactiveTick() {
    }

    @Override
    public void stop() {
    }
}
