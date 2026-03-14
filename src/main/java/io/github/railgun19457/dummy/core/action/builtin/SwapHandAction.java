package io.github.railgun19457.dummy.core.action.builtin;

import io.github.railgun19457.dummy.compat.api.FakePlayerHandle;
import io.github.railgun19457.dummy.core.action.ActionRequest;
import io.github.railgun19457.dummy.core.action.ActionResult;
import io.github.railgun19457.dummy.core.action.ActionType;
import io.github.railgun19457.dummy.core.action.DummyAction;
import io.github.railgun19457.dummy.core.port.CompatBridge;
import io.github.railgun19457.dummy.core.service.DummyLifecycleService;

public final class SwapHandAction implements DummyAction {

    private final CompatBridge compatBridge;
    private final DummyLifecycleService lifecycleService;

    public SwapHandAction(CompatBridge compatBridge, DummyLifecycleService lifecycleService) {
        this.compatBridge = compatBridge;
        this.lifecycleService = lifecycleService;
    }

    @Override
    public ActionType type() {
        return ActionType.SWAP_HAND;
    }

    @Override
    public ActionResult validate(ActionRequest request) {
        return ActionResult.success("dummy.action.swap_hand.success");
    }

    @Override
    public ActionResult execute(ActionRequest request, FakePlayerHandle fakePlayerHandle) {
        compatBridge.swapHands(fakePlayerHandle);
        lifecycleService.swapHandItems(request.dummyId());
        return ActionResult.success("dummy.action.swap_hand.success");
    }
}
