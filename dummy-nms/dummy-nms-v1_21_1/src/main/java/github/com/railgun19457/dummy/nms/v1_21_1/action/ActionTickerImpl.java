package github.com.railgun19457.dummy.nms.v1_21_1.action;

import github.com.railgun19457.dummy.nms.NMSServerPlayer;
import github.com.railgun19457.dummy.nms.action.Action;
import github.com.railgun19457.dummy.nms.action.ActionSetting;
import github.com.railgun19457.dummy.nms.action.ActionType;
import org.jetbrains.annotations.NotNull;

public class ActionTickerImpl extends BaseActionTicker {

    public ActionTickerImpl(@NotNull NMSServerPlayer player, @NotNull ActionType type, @NotNull ActionSetting setting) {
        super(player, type, setting);
    }

    @Override
    protected Action createAction(@NotNull NMSServerPlayer player, @NotNull ActionType type) {
        return switch (type) {
            case JUMP -> new JumpAction(player);
            case LOOK_AT_ENTITY -> new LookAtEntityAction(player);
            case DROP_ITEM -> new DropItemAction(player);
            case DROP_STACK -> new DropStackAction(player);
            case ATTACK -> new AttackAction(player.getPlayer());
            case MINE -> new MineAction(player.getPlayer());
            case USE -> new UseAction(player.getPlayer());
        };
    }
}
