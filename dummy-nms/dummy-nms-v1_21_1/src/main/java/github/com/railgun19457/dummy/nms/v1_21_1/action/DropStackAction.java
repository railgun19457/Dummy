package github.com.railgun19457.dummy.nms.v1_21_1.action;

import github.com.railgun19457.dummy.nms.NMSServerPlayer;
import github.com.railgun19457.dummy.nms.action.Action;
import org.jetbrains.annotations.NotNull;

public class DropStackAction implements Action {

    private final NMSServerPlayer player;

    public DropStackAction(@NotNull NMSServerPlayer player) {
        this.player = player;
    }

    @Override
    public boolean tick() {
        player.getPlayer().dropItem(true);
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
