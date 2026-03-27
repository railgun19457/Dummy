package github.com.railgun19457.dummy.nms.v1_21_1.action;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AttackAction extends TraceAction {

    public AttackAction(@NotNull Player player) {
        super(player);
    }

    @Override
    public boolean tick() {
        var hit = getTarget();
        if (hit == null || hit.getType() != HitResult.Type.ENTITY) {
            return false;
        }

        var entityHit = (EntityHitResult) hit;
        player.attack(entityHit.getEntity());
        player.swing(InteractionHand.MAIN_HAND);
        player.resetAttackStrengthTicker();
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
