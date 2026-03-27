package github.com.railgun19457.dummy.nms.v1_21_1.action;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UseAction extends TraceAction {

    public UseAction(@NotNull Player player) {
        super(player);
    }

    @Override
    public boolean tick() {
        var hit = getTarget();
        if (hit == null) {
            return false;
        }

        if (hit.getType() == HitResult.Type.BLOCK) {
            var blockHit = (BlockHitResult) hit;
            player.gameMode.useItemOn(player, player.level(), player.getItemInHand(InteractionHand.MAIN_HAND), InteractionHand.MAIN_HAND, blockHit);
        } else {
            player.gameMode.useItem(player, player.level(), player.getItemInHand(InteractionHand.MAIN_HAND), InteractionHand.MAIN_HAND);
        }

        player.swing(InteractionHand.MAIN_HAND);
        player.resetLastActionTime();
        return true;
    }

    @Override
    public void inactiveTick() {
        if (player.isUsingItem()) {
            player.stopUsingItem();
        }
    }

    @Override
    public void stop() {
        if (player.isUsingItem()) {
            player.stopUsingItem();
        }
    }
}
