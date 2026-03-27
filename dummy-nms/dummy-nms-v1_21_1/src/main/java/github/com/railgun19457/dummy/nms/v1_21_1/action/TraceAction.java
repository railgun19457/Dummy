package github.com.railgun19457.dummy.nms.v1_21_1.action;

import github.com.railgun19457.dummy.nms.action.Action;
import github.com.railgun19457.dummy.nms.v1_21_1.action.util.Tracer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.HitResult;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class TraceAction implements Action {

    protected final ServerPlayer player;

    protected TraceAction(@NotNull Player player) {
        this.player = ((CraftPlayer) player).getHandle();
    }

    @Nullable
    protected HitResult getTarget() {
        double reach = player.gameMode.isCreative() ? 5 : 4.5;
        return Tracer.rayTrace(player, 1, reach, false);
    }
}
