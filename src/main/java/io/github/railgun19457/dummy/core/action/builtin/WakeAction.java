package io.github.railgun19457.dummy.core.action.builtin;

import io.github.railgun19457.dummy.compat.api.FakePlayerHandle;
import io.github.railgun19457.dummy.core.action.ActionRequest;
import io.github.railgun19457.dummy.core.action.ActionResult;
import io.github.railgun19457.dummy.core.action.ActionType;
import io.github.railgun19457.dummy.core.action.DummyAction;
import io.github.railgun19457.dummy.core.model.DummyRuntimeState;
import io.github.railgun19457.dummy.core.port.CompatBridge;
import io.github.railgun19457.dummy.core.service.DummyLifecycleService;

public final class WakeAction implements DummyAction {

    private final CompatBridge compatBridge;
    private final DummyLifecycleService lifecycleService;

    public WakeAction(CompatBridge compatBridge, DummyLifecycleService lifecycleService) {
        this.compatBridge = compatBridge;
        this.lifecycleService = lifecycleService;
    }

    @Override
    public ActionType type() {
        return ActionType.WAKE;
    }

    @Override
    public ActionResult validate(ActionRequest request) {
        return ActionResult.success("dummy.action.wake.success");
    }

    @Override
    public ActionResult execute(ActionRequest request, FakePlayerHandle fakePlayerHandle) {
        compatBridge.wakeUp(fakePlayerHandle);
        lifecycleService.updateRuntimeState(request.dummyId(), currentState -> new DummyRuntimeState(
                currentState.dummyId(),
                currentState.spawned(),
                currentState.entityId(),
                ActionType.WAKE,
                false,
                currentState.sneaking(),
                currentState.sprinting(),
                currentState.mountedTarget()
        ));
        return ActionResult.success("dummy.action.wake.success");
    }
}
