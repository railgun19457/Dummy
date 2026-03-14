package io.github.railgun19457.dummy.core.action.builtin;

import io.github.railgun19457.dummy.compat.api.FakePlayerHandle;
import io.github.railgun19457.dummy.core.action.ActionRequest;
import io.github.railgun19457.dummy.core.action.ActionResult;
import io.github.railgun19457.dummy.core.action.ActionType;
import io.github.railgun19457.dummy.core.action.DummyAction;
import io.github.railgun19457.dummy.core.port.CompatBridge;
import io.github.railgun19457.dummy.core.service.DummyLifecycleService;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class HoldItemAction implements DummyAction {

    private final CompatBridge compatBridge;
    private final DummyLifecycleService lifecycleService;

    public HoldItemAction(CompatBridge compatBridge, DummyLifecycleService lifecycleService) {
        this.compatBridge = compatBridge;
        this.lifecycleService = lifecycleService;
    }

    @Override
    public ActionType type() {
        return ActionType.HOLD_ITEM;
    }

    @Override
    public ActionResult validate(ActionRequest request) {
        return request.argument("material") instanceof Material && request.argument("amount") instanceof Integer
                ? ActionResult.success("dummy.action.hold_item.success")
                : ActionResult.failure("dummy.action.invalid-args");
    }

    @Override
    public ActionResult execute(ActionRequest request, FakePlayerHandle fakePlayerHandle) {
        Material material = (Material) request.argument("material");
        int amount = (Integer) request.argument("amount");
        ItemStack itemStack = new ItemStack(material, amount);
        compatBridge.holdItem(fakePlayerHandle, itemStack);
        lifecycleService.setHeldItem(request.dummyId(), itemStack);
        return ActionResult.success("dummy.action.hold_item.success");
    }
}
