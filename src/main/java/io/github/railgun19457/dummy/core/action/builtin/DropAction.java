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

public final class DropAction implements DummyAction {

    private final CompatBridge compatBridge;
    private final DummyLifecycleService lifecycleService;

    public DropAction(CompatBridge compatBridge, DummyLifecycleService lifecycleService) {
        this.compatBridge = compatBridge;
        this.lifecycleService = lifecycleService;
    }

    @Override
    public ActionType type() {
        return ActionType.DROP;
    }

    @Override
    public ActionResult validate(ActionRequest request) {
        return ActionResult.success("dummy.action.drop.success");
    }

    @Override
    public ActionResult execute(ActionRequest request, FakePlayerHandle fakePlayerHandle) {
        compatBridge.holdItem(fakePlayerHandle, new ItemStack(Material.AIR));
        lifecycleService.dropMainHandItem(request.dummyId());
        return ActionResult.success("dummy.action.drop.success");
    }
}
