package github.com.railgun19457.dummy.nms.v1_21_1.action;

import github.com.railgun19457.dummy.nms.NMSServerPlayer;
import github.com.railgun19457.dummy.nms.action.Action;
import org.jetbrains.annotations.NotNull;

public class JumpAction implements Action {

    private final NMSServerPlayer player;

    public JumpAction(@NotNull NMSServerPlayer player) {
        this.player = player;
    }

    @Override
    public boolean tick() {
        if (player.onGround()) {
            player.jumpFromGround();
            player.resetLastActionTime();
            return true;
        }
        return false;
    }

    @Override
    public void inactiveTick() {
        player.setJumping(false);
    }

    @Override
    public void stop() {
        player.setJumping(false);
    }
}
